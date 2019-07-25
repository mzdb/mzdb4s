package com.github.mzdb4s.io.reader.bb2

import java.nio.ByteBuffer
import java.util

import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder.ISpectrumDataAdder

import scala.collection.{IndexedSeq, Iterator, SeqLike}
import scala.collection.mutable.LongMap

/**
  * @author marco This implementation is mainly used is mzDbReader
  * <p>Use a ByteBuffer to store the blob's bytes This class extends AbstractBlobReader.</p>
  */
class BoundingBoxBytesReader(
  val bbId: Int,
  private val bytes: Array[Byte],
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  val spectrumHeaderById: LongMap[SpectrumHeader],
  val dataEncodingBySpectrumId: LongMap[DataEncoding]
)(implicit protected val bbIdxFactory: BoundingBoxIndexFactory) extends AbstractBoundingBoxReader {
  //extends AbstractBlobReader(firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId) {

  private val _firstDataEnconding: DataEncoding = dataEncodingBySpectrumId.valuesIterator.next
  private val _bbByteBuffer = ByteBuffer.wrap(bytes).order(_firstDataEnconding.byteOrder)
  private val _blobSize = bytes.length
  //logger.debug("BytesReader: blobSize="+ _blobSize);

  protected val _bbIndex: BoundingBoxIndex = {
    bbIdxFactory.acquireIndex(dataEncodingBySpectrumId).indexSpectrumSlices(
      firstSpectrumId,
      lastSpectrumId,
      new ByteBufferAsInts(_bbByteBuffer),
      _blobSize
    )
  }

  override def dispose(): Unit = {
    super.dispose()
  }

  def getBlobSize(): Int = _blobSize

  override def getSpectraCount(): Int = _bbIndex.spectraCount

  override def getSpectrumIdAt(idx: Int): Long = {
    this.checkSpectrumIndexRange(idx)
    _getSpectrumIdAt(idx)
  }

  @inline
  private def _getSpectrumIdAt(idx: Int): Long = {
    _bbByteBuffer.getInt(_bbIndex.spectrumSliceStartPositions(idx)).toLong
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
    // Retrieve data encoding
    val de = this.dataEncodingBySpectrumId(spectrumId)

    // Determine peaks bytes length
    val peaksBytesSize = _bbIndex.peaksCounts(idx) * de.getPeakStructSize

    // Skip spectrum id and peaks count (two integers)
    val spectrumSliceStartPos = _bbIndex.spectrumSliceStartPositions(idx) + 8

    // Instantiate a new SpectrumData for the corresponding spectrum slice
    this.readSpectrumSliceData(builder, _bbByteBuffer, spectrumSliceStartPos, peaksBytesSize, de, minMz, maxMz)
  }
}

private[this] final class ByteBufferAsInts(
  val byteBuffer: ByteBuffer
) extends AnyVal with IByteBufferAsInts {

  @inline
  def apply(bytesIndex: Int)(implicit bbIdxFactory: BoundingBoxIndexFactory): Int = {
    byteBuffer.getInt(bytesIndex)
  }
}


