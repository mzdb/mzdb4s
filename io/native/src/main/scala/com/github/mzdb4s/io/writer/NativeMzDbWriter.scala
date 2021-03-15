package com.github.mzdb4s.io.writer

import java.io.File

import scala.scalanative.libc.string
import scala.scalanative.unsafe._

import com.github.sqlite4s._
import com.github.sqlite4s.bindings.sqlite_addons.DESTRUCTOR_TYPE._

import com.github.mzdb4s.db.model.MzDbMetaData
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.CTypes


class NativeMzDbWriter(
  override val dbLocation: File,
  override val metaData: MzDbMetaData,
  override val bbSizes: BBSizes,
  override val isDIA: Boolean = false
)(override implicit val sf: ISQLiteFactory) extends MzDbWriter(dbLocation,metaData,bbSizes,isDIA)(sf) {

  def this(dbPath: String, metaData: MzDbMetaData, bbSizes: BBSizes)(implicit sf: ISQLiteFactory) {
    this(new File(dbPath), metaData, bbSizes)
  }

  override protected def formatDateToISO8601String(date: java.util.Date) : String = {
    com.github.mzdb4s.util.date.DateParser.dateToIsoString(date)
  }
}

/*
/**
  * Allows to create a new mzDB file.
  *
  * @author David Bouyssie
  */
// FIXME: this doesn't seem to work => we might have issues with byte encoding
// TODO: make a small test with a ByteBuffer decoding of what is encoded here
class MzDbWriter(
  val dbLocation: File,
  val metaData: MzDbMetaData,
  val bbSizes: BBSizes,
  val isDIA: Boolean = false
)(implicit val sf: ISQLiteFactory) extends AbstractMzDbWriter {

  def this(dbPath: String, metaData: MzDbMetaData, bbSizes: BBSizes)(implicit sf: ISQLiteFactory) {
    this(new File(dbPath), metaData, bbSizes)
  }

  @throws[SQLiteException]
  protected def insertBoundingBox(bb: BoundingBox): Long = { // --- INSERT BOUNDING BOX --- //

    val spectrumIds = bb.spectrumIds
    val spectrumSlices = bb.spectrumSlices
    val slicesCount = spectrumSlices.length
    val bbPeaksCount = spectrumSlices.map(_.getPeaksCount()).sum

    val dataEnc = bb.dataEncoding
    val pe = dataEnc.getPeakEncoding

    // Retrieve m/z and intensity data
    assert(pe != PeakEncoding.NO_LOSS_PEAK, "The NO_LOSS_PEAK encoding is no yet supported!")

    val peakStructSize: CSize = dataEnc.getPeakStructSize()

    val bbLen = (8L * slicesCount) + (peakStructSize * bbPeaksCount)
    /*println("bbLen: "+bbLen)
    println("peaksCount: " + bbPeaksCount) // 10494
    println("peakStructSize: " + peakStructSize)*/

    //val structSize = sizeof[libmzdb.libmzdb_data_point_64_32_t]
    //println("structSize: "+ structSize)

    val bbPtr = stackalloc[Byte](bbLen) // alloc[Byte](bbLen)
    val bbPtrAsInts = bbPtr.asInstanceOf[Ptr[CInt]]
    var bbCursor = bbPtr

    val hwhmsPtr = stackalloc[CTypes.libmzdb_data_point_32_32_t]
    val hwhmsPtrAsBytes = hwhmsPtr.asInstanceOf[Ptr[Byte]]

    // --- SERIALIZE SPECTRUM SLICES TO BOUNDING BOX BYTES --- //

    var sliceIdx = 0
    while (sliceIdx < slicesCount) {

      val spectrumId = spectrumIds(sliceIdx)
      val spectrumSlice = spectrumSlices(sliceIdx)
      val slicePeaksCount = spectrumSlice.getPeaksCount()

      bbPtrAsInts(0L) = spectrumId.toInt
      bbPtrAsInts(1L) = slicePeaksCount

      bbCursor += 8L

      // TODO: try to decrease code redundancy
      //var addedBytes = 8L
      if (pe == PeakEncoding.HIGH_RES_PEAK) {
        val peakPtr = stackalloc[CTypes.libmzdb_data_point_64_32_t]
        //val bbPtrAsPeaks = bbPtr.cast[Ptr[libmzdb.libmzdb_data_point_64_32_t]]
        val peakPtrAsBytes = peakPtr.asInstanceOf[Ptr[Byte]]

        var i = 0
        while (i < slicePeaksCount) {
          //println("i: " + i)
          peakPtr._1 = spectrumSlice.getMzAt(i)
          peakPtr._2 = spectrumSlice.getIntensityAt(i)
          //peakPtr = bbPtrAsPeaks(i)

          // FIXME: we don't know yet how to disable memory padding with SN => let's use manual memory copy
          string.memcpy(bbCursor, peakPtrAsBytes, 12L) // we use static size to make it a little bit faster
          bbCursor += peakStructSize
          //addedBytes += peakStructSize

          if (dataEnc.mode == DataMode.FITTED) {
            hwhmsPtr._1 = spectrumSlice.getLeftHwhmAt(i).getOrElse(0f)
            hwhmsPtr._2 = spectrumSlice.getRightHwhmAt(i).getOrElse(0f)

            string.memcpy(bbCursor, hwhmsPtrAsBytes, 8L) // we use static size to make it a little bit faster
            bbCursor += 8L
          }

          i += 1
        }

      } else {
        val peakPtr = stackalloc[CTypes.libmzdb_data_point_32_32_t]
        //val bbPtrAsPeaks = bbPtr.asInstanceOf[Ptr[CTypes.libmzdb_data_point_32_32_t]]
        val peakPtrAsBytes = peakPtr.asInstanceOf[Ptr[Byte]]

        var i = 0
        while (i < slicePeaksCount) {
          //println("i: " + i)
          peakPtr._1 = spectrumSlice.getMzAt(i).toFloat
          peakPtr._2 = spectrumSlice.getIntensityAt(i)
          //peakPtr = bbPtrAsPeaks(i)

          // FIXME: we don't know yet how to disable memory padding with SN => let's use manual memory copy
          string.memcpy(bbCursor, peakPtrAsBytes, 8L) // we use static size to make it a little bit faster
          bbCursor += peakStructSize
          //addedBytes += peakStructSize

          if (dataEnc.mode == DataMode.FITTED) {
            hwhmsPtr._1 = spectrumSlice.getLeftHwhmAt(i).getOrElse(0f)
            hwhmsPtr._2 = spectrumSlice.getRightHwhmAt(i).getOrElse(0f)

            string.memcpy(bbCursor, hwhmsPtrAsBytes, 8L) // we use static size to make it a little bit faster
            bbCursor += 8L
          }

          i += 1
        }
      }
      //println("addedBytes: "+addedBytes)

      /*val bbPtr2 = alloc[Byte](bbLen)
      val byteArray = CUtils.bytes2ByteArray(bbPtr, bbLen)
      string.memcpy(bbPtr2, bytes, bbLen)*/

      sliceIdx += 1
    }

    var stmt = this.bboxInsertStmt.asInstanceOf[SQLiteStatementWrapper].stmt
    stmt.bind(1, bbPtr, bbLen.toInt, 0, bbLen.toInt, SQLITE_STATIC) //(1, byteBuffer.array)
    //stmt.bind(1, bytesBuffer.toArray)
    stmt.bind(2, bb.runSliceId)
    stmt.bind(3, spectrumIds.head) // first_spectrum_id
    stmt.bind(4, spectrumIds.last) // last_spectrum_id
    stmt.step()

    val bbId = this.getConnection().getLastInsertId()
    stmt.reset()

    bbId
  }

}*/

