package com.github.mzdb4s.io.reader.bb

import java.util

import scala.collection.mutable.LongMap

import com.github.mzdb4s.msdata._
import com.github.sqlite4s.ISQLiteBlob
import com.github.mzdb4s.util.primitive.BytesUtils

class SQLiteBlobReader(
  private var _blob: ISQLiteBlob,
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  val spectrumHeaderById: LongMap[SpectrumHeader],
  val dataEncodingBySpectrumId: LongMap[DataEncoding]
) extends AbstractBlobReader {
  //extends AbstractBlobReader(firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId) {

  this._indexSpectrumSlices((1 + lastSpectrumId - firstSpectrumId).toInt)

  override def disposeBlob(): Unit = _blob.dispose()

  def getBlobSize(): Int = {
    try _blob.getSize()
    catch {
      case e: Exception =>
        throw new Exception("can't get the SQLite blob size", e)
    }
  }

  override def getSpectraCount(): Int = _spectraCount

  // TODO: factorize this code with the one from BytesReader
  //@throws[StreamCorruptedException]
  protected def _indexSpectrumSlices(estimatedSpectraCount: Int): Unit = {
    val spectrumSliceStartPositions = new Array[Int](estimatedSpectraCount)
    val peaksCounts = new Array[Int](estimatedSpectraCount)
    val size = getBlobSize()

    var spectrumSliceIdx = 0
    var byteIdx = 0
    while (byteIdx < size) { // Retrieve the spectrum id
      val spectrumId = _getIntFromBlob(_blob, byteIdx).toLong
      _spectrumSliceStartPositions(spectrumSliceIdx) = byteIdx
      // spectrumSliceStartPositions.add(byteIdx);

      // Skip the spectrum id bytes
      byteIdx += 4

      // Retrieve the number of peaks
      val peaksCount = _getIntFromBlob(_blob, byteIdx)
      _peaksCounts(spectrumSliceIdx) = peaksCount
      // peaksCounts.add(byteIdx);

      // Skip the peaksCount bytes
      byteIdx += 4

      // Retrieve the DataEncoding corresponding to this spectrum
      val de = this.dataEncodingBySpectrumId(spectrumId)
      this.checkDataEncodingIsNotNull(de, spectrumId)

      byteIdx += peaksCount * de.getPeakStructSize // skip nbPeaks * size of one peak

      spectrumSliceIdx += 1
    } // end of while loop

    this._spectraCount = spectrumSliceIdx
    this._spectrumSliceStartPositions = util.Arrays.copyOf(spectrumSliceStartPositions, _spectraCount)
    this._peaksCounts = util.Arrays.copyOf(peaksCounts, _spectraCount)
    // this._spectraCount = spectrumSliceStartPositions.size();
    // this._spectrumSliceStartPositions = intListToInts(spectrumSliceStartPositions, _spectraCount);
    // this._peaksCounts = intListToInts(peaksCounts, _spectraCount);
  }

  override def getSpectrumIdAt(idx: Int): Long = {
    this.checkSpectrumIndexRange(idx)
    _getSpectrumIdAt(idx)
  }

  private def _getSpectrumIdAt(idx: Int): Long =  {
    _getIntFromBlob(_blob, idx).toLong
  }

  private def _getIntFromBlob(blob: ISQLiteBlob, idx: Int): Int = {
    val byteArray = new Array[Byte](4)

    // read 4 bytes
    try blob.read(idx, byteArray, 0, 4)
    catch {
      case e: Exception =>
        throw new Exception("can't read bytes from the SQLite blob", e)
    }

    BytesUtils.bytesToInt(byteArray, 0)
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

    // Retrieve spectrum data encoding
    val de = this.dataEncodingBySpectrumId(spectrumId)

    // Determine peaks bytes length
    val peaksBytesSize = _peaksCounts(idx) * de.getPeakStructSize

    // Skip spectrum id and peaks count (two integers)
    val spectrumSliceStartPos = _spectrumSliceStartPositions(idx) + 8

    val peaksBytes = new Array[Byte](peaksBytesSize)
    try _blob.read(spectrumSliceStartPos, peaksBytes, 0, peaksBytesSize)
    catch {
      case e: Exception =>
        throw new Exception("can't read bytes from the SQLiteBlob", e)
    }

    // Instantiate a new SpectrumData for the corresponding spectrum slice
    this.readSpectrumSliceData(ByteArrayWrapper(peaksBytes, de.byteOrder), spectrumSliceStartPos, peaksBytesSize, de, minMz, maxMz)
  }
}
