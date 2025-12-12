package com.github.mzdb4s.msdata

import java.util

import scala.beans.BeanProperty
import scala.collection.IndexedSeq
import com.github.mzdb4s.util.ms.MsUtils

trait ISpectrumDataContainer extends Any {
  def getMzAt(index: Int): Double
  def getIntensityAt(index: Int): Float
  def getLeftHwhmAt(index: Int): Option[Float]
  def getRightHwhmAt(index: Int): Option[Float]
  def getPeaksCount(): Int

  def forEachPeak(lcContext: ILcContext)(peakConsumer: (IPeak, Int) => Unit): Unit
  def getMinMz(): Double
  def getMaxMz(): Double
  def isEmpty(): Boolean
}

trait ISpectrumData extends ISpectrumDataContainer {
  // Methods to be implemented
  def mzList: IndexedSeq[Double]
  def intensityList: IndexedSeq[Float]
  def leftHwhmList: IndexedSeq[Float]
  def rightHwhmList: IndexedSeq[Float]
  def peaksCount: Int
  //def toPeaks[T <: IPeak](lcContext: ILcContext): Array[T]
  def toPeaks(lcContext: ILcContext): Array[IPeak]

  // Implemented methods
  def getMzAt(index: Int): Double = mzList(index)
  def getIntensityAt(index: Int): Float = intensityList(index)
  def getLeftHwhmAt(index: Int): Option[Float] = if (leftHwhmList != null) Some(leftHwhmList(index)) else None
  def getRightHwhmAt(index: Int): Option[Float] = if (rightHwhmList != null) Some(rightHwhmList(index)) else None
  def getPeaksCount(): Int = peaksCount
  def getNearestPeak(mz: Double, mzTolPPM: Double, lcContext: ILcContext): IPeak
}

abstract class AbstractSpectrumData extends ISpectrumData {
  /**
    * Gets the min mz.
    *
    * @return the min mz
    */
  def getMinMz(): Double = { // supposed and I hope it will always be true that mzList is sorted
    // do not do any verification
    if (peaksCount == 0) return 0.0
    mzList.head
  }

  /**
    * Gets the max mz.
    *
    * @return the max mz
    */
  def getMaxMz(): Double = {
    if (peaksCount == 0) return 0.0
    mzList(peaksCount - 1)
  }

  /**
    * Checks if is empty.
    *
    * @return true, if is empty
    */
  def isEmpty(): Boolean = { // assuming intensityList and others have the same size
    peaksCount == 0
  }

}

/*
case class SpectrumData(
  @BeanProperty mzList: WrappedArray[Double],
  @BeanProperty intensityList: WrappedArray[Float],
  @BeanProperty leftHwhmList: WrappedArray[Float],
  @BeanProperty rightHwhmList: WrappedArray[Float]
) extends AbstractSpectrumData {
 */

/*case class SpectrumData(
  @BeanProperty mzList: Array[Double],
  @BeanProperty intensityList: Array[Float],
  @BeanProperty leftHwhmList: Array[Float],
  @BeanProperty rightHwhmList: Array[Float]
)*/
case class SpectrumData(
  @BeanProperty mzList: IndexedSeq[Double],
  @BeanProperty intensityList: IndexedSeq[Float],
  @BeanProperty leftHwhmList: IndexedSeq[Float],
  @BeanProperty rightHwhmList: IndexedSeq[Float]
) extends AbstractSpectrumData {
  require(mzList != null, "mzList is null")
  require(intensityList != null, "intensityList is null")

  val peaksCount: Int = mzList.length

  def this(mzList: IndexedSeq[Double], intensityList: IndexedSeq[Float]) {
    this(mzList, intensityList, null, null)
  }

  def forEachPeak(lcContext: ILcContext)(peakConsumer: (IPeak, Int) => Unit): Unit = {

    var i = 0
    while (i < peaksCount) {

      var leftHwhm = 0f
      var rightHwhm = 0f

      if (leftHwhmList != null && rightHwhmList != null) {
        leftHwhm = leftHwhmList(i)
        rightHwhm = rightHwhmList(i)
      }

      peakConsumer.apply(Peak(mzList(i), intensityList(i), leftHwhm, rightHwhm, lcContext), i)

      i += 1
    }

  }

  /**
    * To peaks. A new peaks array is instantiated for each call.
    *
    * @return the peak[]
    */
  def toPeaks(lcContext: ILcContext): Array[IPeak] = {
    val peaks = new Array[IPeak](peaksCount)

    val mzArray = mzList
    val intensityArray = intensityList
    val leftHwhmArray = leftHwhmList
    val rightHwhmArray = rightHwhmList

    var i = 0
    while (i < peaksCount) {
      var leftHwhm = 0f
      var rightHwhm = 0f
      if (leftHwhmArray != null && rightHwhmArray != null) {
        leftHwhm = leftHwhmArray(i)
        rightHwhm = rightHwhmArray(i)
      }
      peaks(i) = Peak(mzArray(i), intensityArray(i), leftHwhm, rightHwhm, lcContext)
      i += 1
    }

    peaks
  }

  /*
  /**
    * Resize data arrays.
    *
    * @param newLength
    * the new length
    */
  def resizeDataArrays(newLength: Int): Unit = {
    this.mzList = util.Arrays.copyOf(this.mzList, newLength)
    this.intensityList = util.Arrays.copyOf(this.intensityList, newLength)
    if (this.leftHwhmList != null && this.rightHwhmList != null) {
      this.leftHwhmList = util.Arrays.copyOf(this.leftHwhmList, newLength)
      this.rightHwhmList = util.Arrays.copyOf(this.rightHwhmList, newLength)
    }
    this.peaksCount = newLength
  }*/

  /** assuming mzList is sorted */
  import scala.collection.Searching._
  def getNearestPeak(mz: Double, mzTolPPM: Double, lcContext: ILcContext): IPeak = {
    if (peaksCount == 0) return null

    /*
    val binSearchIndex = util.Arrays.binarySearch(myMzList, mz)
    val idx = if (binSearchIndex < 0) ~binSearchIndex else binSearchIndex
    */

    val nearestIdx = this.getNearestPeakIndex(mz: Double, mzTolPPM: Double)
    if (nearestIdx == -1) return null

    if (leftHwhmList != null && rightHwhmList != null) {
      Peak(
        this.mzList(nearestIdx),
        this.intensityList(nearestIdx),
        this.leftHwhmList(nearestIdx),
        this.rightHwhmList(nearestIdx),
        lcContext)
    }
    else {
      val peak = new Peak(this.mzList(nearestIdx), this.intensityList(nearestIdx))
      peak.setLcContext(lcContext)
      peak
    }
  }

  // Returns -1 if empty spectrum
  def getNearestPeakIndex(mz: Double): Int = {
    if (peaksCount == 0) return -1

    /*var idx = util.Arrays.binarySearch(myMzList, mz)
    idx = if (idx < 0) ~idx else idx

    val maxIdx = this.peaksCount - 1
    var minDiff = Double.MaxValue // difference to minimize
    var k = Math.max(0, idx - 1)
    while (k <= Math.min(maxIdx, idx + 1)) {
      val mzDiff = Math.abs(myMzList(k) - mz)
      if (mzDiff < minDiff) {
        minDiff = mzDiff
        idx = k
      }
      k += 1
    }
    */

    _binarySearchResultToNearestIndex(this.mzList.search(mz), mz)
  }

  private def _binarySearchResultToNearestIndex(searchResult: SearchResult, mz: Double): Int = {
    val myMzList = this.mzList

    searchResult match {
      case Found(index) => index
      case InsertionPoint(idx) => {
        if (idx == 0) idx
        else if (idx == peaksCount) idx - 1
        else {
          val prevVal = myMzList(idx - 1)
          val nextVal = myMzList(idx)
          val diffPrevVal = Math.abs(mz - prevVal)
          val diffNextVal = Math.abs(mz - nextVal)
          if (diffNextVal < diffPrevVal) idx else idx - 1
        }
      }
    }
  }

  /*private def _binSearchIndexToNearestIndex(binSearchIndex: Int): Int = {
    if (binSearchIndex >= 0) binSearchIndex
    else {
      val idx = -binSearchIndex - 1
      if (idx == 0) -1
      else idx
    }
  }*/

  private def _binarySearchResultToIndex(searchResult: SearchResult): Int = {
    searchResult match {
      case Found(index) => index
      case InsertionPoint(index) => index
    }
  }

  // Returns -1 if no match
  @inline
  def getNearestPeakIndex(mz: Double, mzTolPPM: Double): Int = {
    if (peaksCount == 0) return -1

    val myMzList = this.mzList
    val mzDa = MsUtils.ppmToDa(mz, mzTolPPM)

    /*
    val binSearchIndex = util.Arrays.binarySearch(myMzList, mz)

    val idx = if (binSearchIndex < 0) ~binSearchIndex else binSearchIndex
    */

    val nearestIdx = myMzList.search(mz) match {
      case Found(index) => index
      case InsertionPoint(idx) => {
        // Refine the search to find the closest m/z value
        var newIdx = 0
        if (idx == peaksCount) {
          val prevVal = myMzList(peaksCount - 1)
          if (Math.abs(mz - prevVal) > mzDa) return -1
          newIdx = idx - 1
        }
        else if (idx == 0) { // System.out.println("idx == zero");
          val nextVal = myMzList(idx)
          if (Math.abs(mz - nextVal) > mzDa) return -1
          newIdx = idx
          // System.out.println(""+ this.mzList[idx] +", "+ mz);
        }
        else {
          val prevVal = myMzList(idx - 1)
          val nextVal = myMzList(idx)
          val diffPrevVal = Math.abs(mz - prevVal)
          val diffNextVal = Math.abs(mz - nextVal)
          if (diffNextVal < diffPrevVal) {
            if (diffNextVal > mzDa) return -1
            newIdx = idx
          }
          else {
            if (diffPrevVal > mzDa) return -1
            newIdx = idx - 1
          }
        }

        newIdx
      }
    }

    nearestIdx

    /*var idx = util.Arrays.binarySearch(myMzList, mz)
    idx = if (idx < 0) ~idx else idx

    var min = Double.MaxValue
    var resultIdx = -1
    var k = Math.max(0, idx - 1)
    while (k <= Math.min(this.peaksCount - 1, idx + 1)) {
      if ((1e6 * Math.abs(myMzList(k) - mz) / mz < ppmTol) && (Math.abs(myMzList(k) - mz) < min)) {
        min = Math.abs(myMzList(k) - mz)
        resultIdx = k
      }
      k += 1
    }

    resultIdx*/
  }

  def mzRangeFilter(minMz: Double, maxMz: Double): SpectrumData = {
    require(minMz <= maxMz, "maxMz can be lower than minMz")

    /*val nbPoints = peaksCount

    // Retrieve the index of nearest minimum value if it exists
    val minBinSearchIndex = util.Arrays.binarySearch(myMzList, minMz)
    var firstIdx = this._binSearchIndexToNearestIndex(minBinSearchIndex)
    // If out of bounds => return empty spectrum data
    if (firstIdx == nbPoints) return null
    // If first index => set the first value index as the array first index
    if (firstIdx == -1) firstIdx = 0

    // Retrieve the index of nearest maximum value if it exists
    val maxBinSearchIndex = util.Arrays.binarySearch(myMzList, firstIdx, nbPoints, maxMz)
    val lastIdx = this._binSearchIndexToNearestIndex(maxBinSearchIndex)
    // If first index => return empty spectrum data
    if (lastIdx == -1) return null
    */

    // Retrieve the index of nearest minimum value if it exists
    val firstIdx = _binarySearchResultToIndex(this.mzList.search(minMz))
    if (firstIdx == peaksCount) return null

    // Retrieve the index of nearest maximum value if it exists
    val lastIdx = _binarySearchResultToIndex(
      this.mzList.search(maxMz, firstIdx, peaksCount)
    )
    if (lastIdx == 0 && this.mzList(0) > maxMz) return null

    val filteredSpectrumData = if (this.leftHwhmList == null || this.rightHwhmList == null) {
      new SpectrumData(
        this.mzList.slice(firstIdx, lastIdx),
        this.intensityList.slice(firstIdx, lastIdx)
      )
    } else {
      new SpectrumData(
        this.mzList.slice(firstIdx, lastIdx),
        this.intensityList.slice(firstIdx, lastIdx),
        this.leftHwhmList.slice(firstIdx, lastIdx),
        this.rightHwhmList.slice(firstIdx, lastIdx)
      )
    }

    filteredSpectrumData
  }
}