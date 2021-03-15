package com.github.mzdb4s.io.reader.bb

import com.github.mzdb4s.msdata._

/**
  * Abstract Class containing commons objects and attributes through the implementations
  *
  * @author David Bouyssie
  * @see IBlobReader
  */
/*object AbstractBlobReader {
  /** Size of structure depending on selected dataMode **/
  private[bb] val FITTED = 20
  private[bb] val CENTROID = 12
}*/

abstract class AbstractBlobReader /*protected(
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  var _spectrumHeaderById: collection.mutable.LongMap[SpectrumHeader],
  var _dataEncodingBySpectrumId: collection.mutable.LongMap[DataEncoding] // DataEncoding (32-64 bit, centroid/profile)
)*/ extends IBlobReader {

  def firstSpectrumId: Long
  def lastSpectrumId: Long
  def spectrumHeaderById: collection.mutable.LongMap[SpectrumHeader]
  def dataEncodingBySpectrumId: collection.mutable.LongMap[DataEncoding] // DataEncoding (32-64 bit, centroid/profile)

  require(firstSpectrumId <= lastSpectrumId, "lastSpectrumId must be greater or the same than firstSpectrumId")
  protected var _spectraCount = 0 // number of spectrum slices in the blob
  protected var _spectrumSliceStartPositions: Array[Int] = null // list of spectrum slice starting positions in the blob
  protected var _peaksCounts: Array[Int] = null // number of peaks in each spectrum slice of the blob

  override def getAllSpectrumIds(): Array[Long] = {
    val spectraCount = this.getSpectraCount()
    val spectrumIds = new Array[Long](spectraCount)

    var i = 0
    while ( {i < spectraCount}) {
      spectrumIds(i) = this.getSpectrumIdAt(i)
      i += 1
    }

    spectrumIds
  }

  /**
    * Read spectrum slice data by using a IByteArrayWrapper as input
    *
    * @param byteArrayWrapper      array of bytes containing the SpectrumSlices of interest
    * @param spectrumSliceStartPos the starting position
    * @param peaksBytesLength      length of bytes used by peaks
    * @param de                    the corresponding DataEncoding
    * @param minMz                 the minimum m/z value
    * @param maxMz                 the maximum m/z value
    * @return
    */
  protected def readSpectrumSliceData(
    byteArrayWrapper: IByteArrayWrapper,
    spectrumSliceStartPos: Int,
    peaksBytesLength: Int,
    de: DataEncoding,
    minMz: Double,
    maxMz: Double
  ): SpectrumData = {

    //return new SpectrumData(Array(), Array())

    val dataMode = de.getMode
    val pe = de.getPeakEncoding
    val structSize = de.getPeakStructSize()

    var peaksCount = 0
    var peaksStartIdx = 0
    // If no m/z range is provided
    if (minMz < 0 && maxMz < 0) { // Compute the peaks count for the whole spectrum slice
      peaksCount = peaksBytesLength / structSize
      // Set peaksStartIdx value to spectrumSliceStartPos
      peaksStartIdx = spectrumSliceStartPos
      // Else determine the peaksStartIdx and peaksCount corresponding to provided m/z filters
    }
    else {
      // Determine the max m/z threshold to use
      var maxMzThreshold = maxMz
      if (maxMz < 0) maxMzThreshold = Double.MaxValue

      var i = 0
      while (i < peaksBytesLength) {
        val peakStartPos = spectrumSliceStartPos + i
        val mz = pe match {
          case PeakEncoding.HIGH_RES_PEAK =>
            byteArrayWrapper.getDouble(peakStartPos)
          case PeakEncoding.LOW_RES_PEAK =>
            byteArrayWrapper.getFloat(peakStartPos).toDouble
          case PeakEncoding.NO_LOSS_PEAK =>
            byteArrayWrapper.getDouble(peakStartPos)
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

    // Set the position of the byte array
    byteArrayWrapper.position(peaksStartIdx)

    // Create new arrays of primitives
    val mzArray = new Array[Double](peaksCount)
    val intensityArray = new Array[Float](peaksCount)
    val lwhmArray = new Array[Float](peaksCount)
    val rwhmArray = new Array[Float](peaksCount)

    var peakIdx = 0
    while (peakIdx < peaksCount) {
      pe match {
        case PeakEncoding.HIGH_RES_PEAK =>
          mzArray(peakIdx) = byteArrayWrapper.getDouble()
          intensityArray(peakIdx) = byteArrayWrapper.getFloat()
        case PeakEncoding.LOW_RES_PEAK =>
          mzArray(peakIdx) = byteArrayWrapper.getFloat().toDouble
          intensityArray(peakIdx) = byteArrayWrapper.getFloat()
        case PeakEncoding.NO_LOSS_PEAK =>
          mzArray(peakIdx) = byteArrayWrapper.getDouble()
          intensityArray(peakIdx) = byteArrayWrapper.getDouble().toFloat
      }
      if (dataMode == DataMode.FITTED) {
        lwhmArray(peakIdx) = byteArrayWrapper.getFloat()
        rwhmArray(peakIdx) = byteArrayWrapper.getFloat()
      }

      peakIdx += 1
    }

    // return the newly formed SpectrumData
    SpectrumData(mzArray, intensityArray, lwhmArray, rwhmArray)
  }

  @throws[IndexOutOfBoundsException]
  protected def checkSpectrumIndexRange(idx: Int): Unit = {
    if (idx < 0 || idx >= this.getSpectraCount)
      throw new IndexOutOfBoundsException(s"spectrum index out of bounds (idx=$idx), index counting starts at 0")
  }

  //@throws[StreamCorruptedException]
  protected def checkDataEncodingIsNotNull(de: DataEncoding, spectrumId: Long): Unit = if (de == null) {
    //throw new StreamCorruptedException(s"Scared that the mzdb file is corrupted, spectrum id is: $spectrumId")
    throw new Exception(s"Scared that the mzdb file is corrupted, spectrum id is: $spectrumId")
  }

  override def readAllSpectrumSlices(runSliceId: Int): Array[SpectrumSlice] = {
    val spectraCount = this.getSpectraCount()
    val sl = new Array[SpectrumSlice](spectraCount)

    /*println("spectrum IDs:",firstSpectrumId,lastSpectrumId)
    println("runSliceId:" + runSliceId)
    println("spectraCount: " + spectraCount)*/

    /*val startTime = System.currentTimeMillis
    var j = 0

    while (j < 10000) {
      j += 1

      var i = 0
      while (i < spectraCount) {
        val s = this.readSpectrumSliceAt(i)
        s.setRunSliceId(runSliceId)
        sl(i) = s
        i += 1
      }
    }

    val took = System.currentTimeMillis - startTime
    println("readSpectrumSliceAt took : " + took)*/

    var i = 0
    while (i < spectraCount) {
      val s = this.readSpectrumSliceAt(i)
      s.setRunSliceId(runSliceId)
      sl(i) = s
      i += 1
    }

    sl
  }

  // TODO: temp workaround (remove me when each BB is annotated with the number of spectra it contains)
  /*protected int[] intListToInts(List<Integer> integers, int size) {
      int[] ret = new int[size];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = integers.get(i).intValue();
      }
      return ret;
    }*/
}