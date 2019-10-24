package com.github.mzdb4s.io.reader.bb2

import java.nio.ByteBuffer

import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder._
import com.github.mzdb4s.util.primitive.BytesUtils
import com.github.sqlite4s.ISQLiteBlob

import scala.collection.mutable.LongMap

class BoundingBoxBlobReader(
  val bbId: Int,
  private var _blob: ISQLiteBlob,
  val runSliceId: Int,
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  val spectrumHeaderById: LongMap[SpectrumHeader],
  val dataEncodingBySpectrumId: LongMap[DataEncoding]
)(implicit protected val bbIdxFactory: BoundingBoxIndexFactory) extends AbstractBoundingBoxReader {
  //extends AbstractBlobReader(firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId) {

  protected val _bbIndex: BoundingBoxIndex = {
    bbIdxFactory.acquireIndex(dataEncodingBySpectrumId).indexSpectrumSlices(
      firstSpectrumId,
      lastSpectrumId,
      new SQLiteBlobAsInts(_blob),
      getBlobSize()
    )
  }

  override def dispose(): Unit = {
    super.dispose()
  }

  def getBlobSize(): Int = {
    try _blob.getSize()
    catch {
      case e: Exception =>
        throw new Exception("can't get the SQLite blob size", e)
    }
  }

  override def getSpectraCount(): Int = _bbIndex.spectraCount

  override def getSpectrumIdAt(idx: Int): Long = {
    this.checkSpectrumIndexRange(idx)
    _getSpectrumIdAt(idx)
  }

  @inline
  private def _getSpectrumIdAt(idx: Int): Long = {
    SQLiteBlobAsInts.getIntFromBlob(_blob, _bbIndex.spectrumSliceStartPositions(idx), bbIdxFactory.reusableIntBuffer)
  }

  /*override def readSpectrumSliceAt(idx: Int): SpectrumSlice = {
    val spectrumId = _getSpectrumIdAt(idx)
    val spectrumSliceData = this._readFilteredSpectrumSliceDataAt(idx, spectrumId, -1.0, -1.0)
    val sh = spectrumHeaderById(spectrumId)

    // Instantiate a new SpectrumSlice
    SpectrumSlice(sh, spectrumSliceData)
  }*/

  override def readSpectrumSliceDataAt(idx: Int, builder: ISpectrumDataAdder): Unit = {
    this._readFilteredSpectrumSliceDataAt(idx, _getSpectrumIdAt(idx), -1.0, -1.0, builder)
  }

  override def readFilteredSpectrumSliceDataAt(idx: Int, minMz: Double, maxMz: Double, builder: ISpectrumDataAdder): Unit = {
    this._readFilteredSpectrumSliceDataAt(idx, _getSpectrumIdAt(idx), minMz, maxMz, builder)
  }

  private def _readFilteredSpectrumSliceDataAt(idx: Int, spectrumId: Long, minMz: Double, maxMz: Double, builder: ISpectrumDataAdder): Unit = {
    // Retrieve spectrum data encoding
    val de = this.dataEncodingBySpectrumId(spectrumId)

    // Determine peaks bytes length
    val peaksBytesSize = _bbIndex.peaksCounts(idx) * de.getPeakStructSize

    // Skip spectrum id and peaks count (two integers)
    val spectrumSliceStartPos = _bbIndex.spectrumSliceStartPositions(idx) + 8

    //val peaksBytes = new Array[Byte](peaksBytesSize)
    val peaksBytes = bbIdxFactory.acquirePeaksBuffer(peaksBytesSize)
    try _blob.read(spectrumSliceStartPos, peaksBytes, 0, peaksBytesSize)
    catch {
      case e: Exception =>
        throw new Exception("can't read bytes from the SQLiteBlob", e)
    }

    // Instantiate a new SpectrumData for the corresponding spectrum slice
    this.readSpectrumSliceData(builder, ByteBuffer.wrap(peaksBytes), 0, peaksBytesSize, de, minMz, maxMz)
  }
}


private[this] object SQLiteBlobAsInts {

  @inline
  def getIntFromBlob(blob: ISQLiteBlob, idx: Int, byteBuffer: Array[Byte]): Int = {
    // read 4 bytes
    try blob.read(idx, byteBuffer, 0, 4)
    catch {
      case e: Exception =>
        throw new Exception("can't read bytes from the SQLite blob", e)
    }

    BytesUtils.bytesToInt(byteBuffer, 0)
  }
}


private[this] final class SQLiteBlobAsInts(
  val blob: ISQLiteBlob
) extends AnyVal with IByteBufferAsInts {

  @inline
  def apply(bytesIndex: Int)(implicit bbIdxFactory: BoundingBoxIndexFactory): Int = {
    SQLiteBlobAsInts.getIntFromBlob(blob, bytesIndex, bbIdxFactory.reusableIntBuffer)
  }

}