package com.github.mzdb4s.io.writer

import java.io.File
import java.nio.ByteBuffer

import com.github.mzdb4s.db.model.MzDbMetaData
import com.github.mzdb4s.io.writer.{BoundingBox => WriterBB}
import com.github.mzdb4s.msdata._
import com.github.sqlite4s._

class MzDbWriter(
  val dbLocation: File,
  val metaData: MzDbMetaData,
  val bbSizes: BBSizes,
  val isDIA: Boolean = false
)(implicit val sf: ISQLiteFactory) extends AbstractMzDbWriter {

  def this(dbPath: String, metaData: MzDbMetaData, bbSizes: BBSizes)(implicit sf: ISQLiteFactory) {
    this(new File(dbPath), metaData, bbSizes)
  }

  protected def formatDateToISO8601String(date: java.util.Date) : String = {
    com.github.mzdb4s.util.date.DateParser.dateToIsoString(date)
  }

  protected def insertBoundingBox(bb: WriterBB): Long = { // --- INSERT BOUNDING BOX --- //

    val spectrumIds = bb.spectrumIds
    val spectrumSlices = bb.spectrumSlices
    val slicesCount = spectrumSlices.length
    val bbPeaksCount = spectrumSlices.flatMap(_.map(_.peaksCount())).sum

    val dataEnc = bb.dataEncoding
    val pe = dataEnc.getPeakEncoding

    // Retrieve m/z and intensity data
    assert(pe != PeakEncoding.NO_LOSS_PEAK, "The NO_LOSS_PEAK encoding is no yet supported!")

    val peakStructSize = dataEnc.getPeakStructSize()

    val bbLen = (8L * slicesCount) + (peakStructSize * bbPeaksCount)
    /*println("bbLen: "+bbLen)
    println("peaksCount: " + bbPeaksCount) // 10494
    println("peakStructSize: " + peakStructSize)*/

    //val structSize = sizeof[libmzdb.libmzdb_data_point_64_32_t]
    //println("structSize: "+ structSize)

    val bbBytes = new Array[Byte](bbLen.toInt)
    //val bytesBuffer = new scala.collection.mutable.ArrayBuffer[Byte](bbLen.toInt)
    val bytesBuffer = ByteBuffer.wrap(bbBytes).order(dataEnc.byteOrder)

    // --- SERIALIZE SPECTRUM SLICES TO BOUNDING BOX BYTES --- //
    var sliceIdx = 0
    while (sliceIdx < slicesCount) {

      val spectrumId = spectrumIds(sliceIdx)
      bytesBuffer.putInt(spectrumId.toInt)

      val spectrumSliceOpt = spectrumSlices(sliceIdx)

      if (spectrumSliceOpt.isEmpty) bytesBuffer.putInt(0)
      else {
        val spectrumSliceIdx = spectrumSliceOpt.get

        val spectrumData = spectrumSliceIdx.spectrumData
        val firstPeakIdx = spectrumSliceIdx.firstPeakIdx
        val lastPeakIdx = spectrumSliceIdx.lastPeakIdx

        val slicePeaksCount = spectrumSliceIdx.peaksCount()
        bytesBuffer.putInt(slicePeaksCount)

        var i = firstPeakIdx
        while (i <= lastPeakIdx) {

          if (pe == PeakEncoding.HIGH_RES_PEAK) {
            bytesBuffer.putDouble(spectrumData.getMzAt(i))
          } else {
            bytesBuffer.putFloat(spectrumData.getMzAt(i).toFloat)
          }

          bytesBuffer.putFloat(spectrumData.getIntensityAt(i))

          if (dataEnc.mode == DataMode.FITTED) {
            bytesBuffer.putFloat(spectrumData.getLeftHwhmAt(i).getOrElse(0f))
            bytesBuffer.putFloat(spectrumData.getRightHwhmAt(i).getOrElse(0f))
          }

          i += 1
        }
      }

      sliceIdx += 1
    }

    val stmt = this.bboxInsertStmt //.asInstanceOf[SQLiteStatementWrapper].stmt
    stmt.bind(1, bbBytes)
    stmt.bind(2, bb.runSliceId)
    stmt.bind(3, spectrumIds.head) // first_spectrum_id
    stmt.bind(4, spectrumIds.last) // last_spectrum_id
    stmt.step()

    val bbId = this.getConnection().getLastInsertId()
    stmt.reset()

    bbId
  }

}
