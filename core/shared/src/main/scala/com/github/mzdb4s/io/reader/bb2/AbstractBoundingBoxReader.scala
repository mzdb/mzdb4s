/*package com.github.mzdb4s.io.reader.bb2

import java.nio.ByteBuffer

import scala.collection.mutable.Buffer
import scala.collection.mutable.WrappedArray

import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder._

/**
  * Abstract Class containing commons objects and attributes through the implementations
  *
  * @author David Bouyssie
  * @see IBlobReader
  */
abstract class AbstractBoundingBoxReader extends IBoundingBoxReader {

  protected implicit def bbIdxFactory: BoundingBoxIndexFactory

  def spectrumHeaderById: collection.mutable.LongMap[SpectrumHeader]
  def dataEncodingBySpectrumId: collection.mutable.LongMap[DataEncoding] // DataEncoding (32-64 bit, centroid/profile)

  require(firstSpectrumId <= lastSpectrumId, "lastSpectrumId must be greater or the same than firstSpectrumId")

  protected val _bbIndex: BoundingBoxIndex

  override def dispose(): Unit = {
    this.bbIdxFactory.releaseIndex(_bbIndex)
  }

  override def getPeaksCountAt(idx: Int): Int = {
    this.checkSpectrumIndexRange(idx)
    _bbIndex.peaksCounts(idx)
  }

  override def getAllSpectraIds(spectraIds: WrappedArray[Long]): Unit = {
    val spectraCount = this.getSpectraCount()
    require(spectraIds.length == spectraCount, s"invalid spectraIds.length, expected $spectraCount ids but got ${spectraIds.length}")

    var i = 0
    while (i < spectraCount) {
      spectraIds(i) = this.getSpectrumIdAt(i)
      i += 1
    }

    ()
  }

  /**
    * Read spectrum slice data by using a ByteBuffer as input
    *
    * @param bbByteBuffer          array of bytes containing the SpectrumSlices of interest
    * @param spectrumSliceStartPos , the starting position
    * @param peaksBytesLength      , length of bytes used by peaks
    * @param de                    , the corresponding DataEncoding
    * @param minMz                 , the minimum m/z value
    * @param maxMz                 , the maximum m/z value
    * @return
    */
  protected def readSpectrumSliceData(
    spectrumDataAdder: ISpectrumDataAdder,
    bbByteBuffer: ByteBuffer,
    spectrumSliceStartPos: Int,
    peaksBytesLength: Int,
    de: DataEncoding,
    minMz: Double,
    maxMz: Double
  ): Unit = {

    val dataMode = de.getMode
    val pe = de.getPeakEncoding
    val structSize = de.getPeakStructSize

    var peaksCount = 0
    var peaksStartIdx = 0
    // If no m/z range is provided
    if (minMz < 0 && maxMz < 0) { // Compute the peaks count for the whole spectrum slice
      peaksCount = peaksBytesLength / structSize
      // Set peaksStartIdx value to spectrumSliceStartPos
      peaksStartIdx = spectrumSliceStartPos
      // Else determine the peaksStartIdx and peaksCount corresponding to provided m/z filters
    }
    else { // Determine the max m/z threshold to use
      var maxMzThreshold = maxMz
      if (maxMz < 0) maxMzThreshold = Double.MaxValue

      var i = 0
      while (i < peaksBytesLength) {
        val peakStartPos = spectrumSliceStartPos + i
        val mz = pe match {
          case PeakEncoding.HIGH_RES_PEAK =>
            bbByteBuffer.getDouble(peakStartPos)
          case PeakEncoding.LOW_RES_PEAK =>
            bbByteBuffer.getFloat(peakStartPos).toDouble
          case PeakEncoding.NO_LOSS_PEAK =>
            bbByteBuffer.getDouble(peakStartPos)
        }

        // Check if we are in the desired m/z range
        if (mz >= minMz && mz <= maxMzThreshold) { // Increment the number of peaks to read
          peaksCount += 1
          // Determine the peaksStartIdx
          if (mz >= minMz && peaksStartIdx == 0) peaksStartIdx = peakStartPos
        }

        i += structSize
      }
    }

    // Set the position of the byte buffer
    bbByteBuffer.position(peaksStartIdx)

    var peakIdx = 0
    while (peakIdx < peaksCount) {

      var mz: Double = 0.0
      var intensity: Float = 0f

      pe match {
        case PeakEncoding.HIGH_RES_PEAK =>
          mz = bbByteBuffer.getDouble
          intensity = bbByteBuffer.getFloat
        case PeakEncoding.LOW_RES_PEAK =>
          mz = bbByteBuffer.getFloat.toDouble
          intensity = bbByteBuffer.getFloat
        case PeakEncoding.NO_LOSS_PEAK =>
          mz = bbByteBuffer.getDouble
          intensity = bbByteBuffer.getDouble.toFloat
      }

      if (dataMode == DataMode.FITTED) {
        spectrumDataAdder.addDataPoint(mz, intensity, bbByteBuffer.getFloat, bbByteBuffer.getFloat)
      } else {
        spectrumDataAdder.addDataPoint(mz, intensity)
      }

      peakIdx += 1
    }

    ()
  }

  override def readSpectrumSliceAt(idx: Int)(implicit sdbFactory: SpectrumDataBuilderFactory): SpectrumSlice = {
    val spectrumHeader = spectrumHeaderById(getSpectrumIdAt(idx))
    val slicePeaksCount = getPeaksCountAt(idx)
    val sSliceBuilder = new SpectrumSliceBuilder(spectrumHeader, runSliceId, slicePeaksCount)
    this.readFilteredSpectrumSliceDataAt(idx,-1.0, -1.0, sSliceBuilder)
    sSliceBuilder.result()
  }

  override def readAllSpectrumSlicesData(builders: Seq[ISpectrumDataAdder]): Unit = {
    val spectraCount = this.getSpectraCount()
    assert(builders.length == spectraCount, "the number of builders must match the number of spectra")

    var i = 0
    while (i < spectraCount) {
      this.readSpectrumSliceDataAt(i, builders(i))
      i += 1
    }

  }

  override def readAllSpectrumSlices(buffer: Buffer[SpectrumSlice])(implicit sdbFactory: SpectrumDataBuilderFactory): Unit = {
    val spectraCount = this.getSpectraCount()

    var i = 0
    while (i < spectraCount) {
      buffer += this.readSpectrumSliceAt(i)
      i += 1
    }

    ()
  }

  @throws[IndexOutOfBoundsException]
  protected def checkSpectrumIndexRange(idx: Int): Unit = {
    if (idx < 0 || idx >= this.getSpectraCount)
      throw new IndexOutOfBoundsException(s"spectrum index out of bounds (idx=$idx), index counting starts at 0")
  }

  // TODO: temp workaround (remove me when each BB is annotated with the number of spectra it contains)
  /*protected int[] intListToInts(List<Integer> integers, int size) {
      int[] ret = new int[size];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = integers.get(i).intValue();
      }
      return ret;
    }*/
}*/