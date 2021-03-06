package com.github.mzdb4s.io.reader.bb

import java.util

import scala.collection.mutable.LongMap

import com.github.mzdb4s.msdata._

/**
  * @author David Bouyssie This implementation is mainly used is mzDbReader
  * <p>Use a Byte Array to store the blob's bytes This class extends AbstractBlobReader.</p>
  */
class BytesReader(
  private val bytes: Array[Byte],
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  val spectrumHeaderById: LongMap[SpectrumHeader], val dataEncodingBySpectrumId: LongMap[DataEncoding]
) extends AbstractBlobReader {

  //extends AbstractBlobReader(firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId) {
  private val _firstDataEncoding: DataEncoding = dataEncodingBySpectrumId.valuesIterator.next
  private val _bbByteArrayWrapper: IByteArrayWrapper = ByteArrayWrapper(bytes, _firstDataEncoding.byteOrder)
  private val _blobSize = bytes.length
  //logger.debug("BytesReader: blobSize="+ _blobSize);

  this._indexSpectrumSlices((1 + lastSpectrumId - firstSpectrumId).toInt)

  //@throws[StreamCorruptedException]
  protected def _indexSpectrumSlices(estimatedSpectraCount: Int): Unit = {
    val spectrumSliceStartPositions = new Array[Int](estimatedSpectraCount)
    val peaksCounts = new Array[Int](estimatedSpectraCount)

    var spectrumSliceIdx = 0
    var byteIdx = 0
    while (byteIdx < _blobSize) { // Set the new position to access the byte array
      //println(byteIdx)
      _bbByteArrayWrapper.position(byteIdx)

      // Retrieve the spectrum id
      val spectrumId = _bbByteArrayWrapper.getInt().toLong
      //println("spectrumId",spectrumId)
      spectrumSliceStartPositions(spectrumSliceIdx) = byteIdx

      // Retrieve the number of peaks
      val peaksCount = _bbByteArrayWrapper.getInt()
      peaksCounts(spectrumSliceIdx) = peaksCount
      //println("peaksCount",peaksCount)

      // Retrieve the DataEncoding corresponding to this spectrum
      val de = this.dataEncodingBySpectrumId(spectrumId)
      this.checkDataEncodingIsNotNull(de, spectrumId)
      //println("de.getPeakStructSize",de.getPeakStructSize)

      // Skip the spectrum id, peaksCount and peaks (peaksCount * size of one peak)
      byteIdx += 8 + (peaksCount * de.getPeakStructSize)
      spectrumSliceIdx += 1

    } // end of while loop

    this._spectraCount = spectrumSliceIdx
    this._spectrumSliceStartPositions = util.Arrays.copyOf(spectrumSliceStartPositions, _spectraCount)
    this._peaksCounts = util.Arrays.copyOf(peaksCounts, _spectraCount)
  }

  override def disposeBlob(): Unit = {}

  def getBlobSize(): Int = _blobSize

  override def getSpectraCount(): Int = _spectraCount

  override def getSpectrumIdAt(idx: Int): Long = {
    this.checkSpectrumIndexRange(idx)
    _getSpectrumIdAt(idx)
  }

  private def _getSpectrumIdAt(idx: Int): Long = {
    _bbByteArrayWrapper.getInt(_spectrumSliceStartPositions(idx)).toLong
  }

  override def readSpectrumSliceAt(idx: Int): SpectrumSlice = {
    val spectrumId = _getSpectrumIdAt(idx)
    val spectrumSliceData = this._readFilteredSpectrumSliceDataAt(idx, spectrumId, -1.0, -1.0)
    val sh = spectrumHeaderById(spectrumId)
    // Instantiate a new SpectrumSlice
    SpectrumSlice(sh, spectrumSliceData)
  }

  override def readSpectrumSliceDataAt(idx: Int): SpectrumData = {
    this._readFilteredSpectrumSliceDataAt(idx, _getSpectrumIdAt(idx), -1.0, -1.0)
  }

  override def readFilteredSpectrumSliceDataAt(idx: Int, minMz: Double, maxMz: Double): SpectrumData = {
    this._readFilteredSpectrumSliceDataAt(idx, _getSpectrumIdAt(idx), minMz, maxMz)
  }

  private def _readFilteredSpectrumSliceDataAt(idx: Int, spectrumId: Long, minMz: Double, maxMz: Double): SpectrumData = {
    // Retrieve data encoding
    val de = this.dataEncodingBySpectrumId(spectrumId)

    // Determine peaks bytes length
    val peaksBytesSize = _peaksCounts(idx) * de.getPeakStructSize

    // Skip spectrum id and peaks count (two integers)
    val spectrumSliceStartPos = _spectrumSliceStartPositions(idx) + 8

    // Instantiate a new SpectrumData for the corresponding spectrum slice
    this.readSpectrumSliceData(_bbByteArrayWrapper, spectrumSliceStartPos, peaksBytesSize, de, minMz, maxMz)
  }
}