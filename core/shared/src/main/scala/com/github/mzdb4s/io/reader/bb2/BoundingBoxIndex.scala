package com.github.mzdb4s.io.reader.bb2

import com.github.mzdb4s.msdata.DataEncoding
import com.github.mzdb4s.util.collection.ResizedArray

import scala.collection.mutable.LongMap
import scala.collection.mutable.WrappedArray

private[bb2] class BoundingBoxIndex(
  dataEncodingBySpectrumId: LongMap[DataEncoding]
)(implicit bbIdxFactory: BoundingBoxIndexFactory) {

  protected var _spectraCount = 0 // number of spectrum slices in the blob
  protected var _spectrumSliceStartPositions: WrappedArray[Int] = _ // list of spectrum slice starting positions in the blob
  protected var _peaksCounts: WrappedArray[Int] = _ // number of peaks in each spectrum slice of the blob

  private def _ensureSpectraCount(spectraCount: Int): Unit = {
    if (_spectrumSliceStartPositions == null || spectrumSliceStartPositions.length < spectraCount) _spectrumSliceStartPositions = new Array[Int](spectraCount)
    if (_peaksCounts == null || _peaksCounts.length < spectraCount) _peaksCounts = new Array[Int](spectraCount)
  }

  @inline def spectraCount: Int = _spectraCount
  @inline def spectrumSliceStartPositions: WrappedArray[Int] = _spectrumSliceStartPositions
  @inline def peaksCounts: WrappedArray[Int] = _peaksCounts

  def indexSpectrumSlices(
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    bufferAsInts: IByteBufferAsInts,
    nBytes: Int
  ): BoundingBoxIndex = {

    val estimatedSpectraCount = (1 + lastSpectrumId - firstSpectrumId).toInt
    this._ensureSpectraCount(estimatedSpectraCount)

    var spectrumSliceIdx = 0
    var byteIdx = 0
    while (byteIdx < nBytes) {

      // Retrieve the spectrum id
      val spectrumId = bufferAsInts(byteIdx)
      _spectrumSliceStartPositions(spectrumSliceIdx) = byteIdx

      // Retrieve the number of peaks
      val peaksCount = bufferAsInts(byteIdx + 4)
      _peaksCounts(spectrumSliceIdx) = peaksCount

      // Retrieve the DataEncoding corresponding to this spectrum
      val de = this.dataEncodingBySpectrumId(spectrumId)
      this.checkDataEncodingIsNotNull(de, spectrumId)

      // Skip the spectrum id, peaksCount and peaks (peaksCount * size of one peak)
      byteIdx += 8 + (peaksCount * de.getPeakStructSize)
      spectrumSliceIdx += 1

    } // end of while loop

    this._spectraCount = spectrumSliceIdx

    // Resize arrays if needed
    if (_spectrumSliceStartPositions.length > _spectraCount)
      _spectrumSliceStartPositions = new ResizedArray.ofInt(_spectrumSliceStartPositions.array, _spectraCount)

    if (_peaksCounts.length > _spectraCount) {
      _peaksCounts = new ResizedArray.ofInt(_peaksCounts.array, _spectraCount)
    }

    this
  }

  //@throws[StreamCorruptedException]
  protected def checkDataEncodingIsNotNull(de: DataEncoding, spectrumId: Long): Unit = {
    if (de == null) {
      //throw new StreamCorruptedException(s"Scared that the mzdb file is corrupted, spectrum id is: $spectrumId")
      throw new Exception(s"Scared that the mzDB file is corrupted, spectrum id is: $spectrumId")
    }
  }

}