package com.github.mzdb4s.msdata.builder

import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection.ResizedArray

trait ISpectrumDataAdder {

  @inline
  def addDataPoint(mz: Double, intensity: Float): this.type

  @inline
  def addDataPoint(mz: Double, intensity: Float, leftHwhm: Float, rightHwhm: Float): this.type

  @inline
  def addData(
    mzList: Seq[Double],
    intensityList: Seq[Float],
    leftHwhmList: Seq[Float],
    rightHwhmList: Seq[Float]
  ): this.type

  @inline
  def addSpectrumData(spectrumData: ISpectrumData): this.type
}

trait ISpectrumDataBuilder extends ISpectrumDataAdder {
  def dataPointsCount: Int
  def result(): ISpectrumData
}

abstract class AbstractSpectrumDataBuilder extends ISpectrumDataBuilder {}

object SpectrumDataBuilder {
  def mergeSpectrumDataList(spectrumDataList: Seq[ISpectrumData], peaksCount: Int): SpectrumData = {
    val finalMzList = new Array[Double](peaksCount)
    val finalIntensityList = new Array[Float](peaksCount)
    var finalLeftHwhmList: Array[Float] = null
    var finalRightHwhmList: Array[Float] = null
    val firstSpectrumData = spectrumDataList.head
    if (firstSpectrumData.leftHwhmList != null && firstSpectrumData.rightHwhmList != null) {
      finalLeftHwhmList = new Array[Float](peaksCount)
      finalRightHwhmList = new Array[Float](peaksCount)
    }

    // TODO: check that spectrumDataList is m/z sorted ???
    var finalPeakIdx = 0
    for (spectrumData <- spectrumDataList) {
      val mzList = spectrumData.mzList
      val intensityList = spectrumData.intensityList
      val leftHwhmList = spectrumData.leftHwhmList
      val rightHwhmList = spectrumData.rightHwhmList
      // Add peaks of this SpectrumData to the final arrays
      val spectrumDataPeaksCount = spectrumData.peaksCount

      var i = 0
      while (i < spectrumDataPeaksCount) {
        finalMzList(finalPeakIdx) = mzList(i)
        finalIntensityList(finalPeakIdx) = intensityList(i)
        if (finalLeftHwhmList != null && finalRightHwhmList != null) {
          finalLeftHwhmList(finalPeakIdx) = leftHwhmList(i)
          finalRightHwhmList(finalPeakIdx) = rightHwhmList(i)
        }
        finalPeakIdx += 1

        i += 1
      }
    }

    SpectrumData(finalMzList, finalIntensityList, finalLeftHwhmList, finalRightHwhmList)
  }
}

class SpectrumDataBuilder(var dataPointsCount: Int) extends AbstractSpectrumDataBuilder {

  private var _index = 0

  private var _mzList = new Array[Double](dataPointsCount)
  private var _intensityList = new Array[Float](dataPointsCount)
  private var _leftHwhmList: Array[Float] = _
  private var _rightHwhmList: Array[Float] = _

  //def length(): Int = _mzList.length

  @inline
  def addDataPoint(mz: Double, intensity: Float): this.type = {
    _mzList(_index) = mz
    _intensityList(_index) = intensity
    _index += 1
    //_capacity += 1
    this
  }

  @inline
  def addDataPoint(mz: Double, intensity: Float, leftHwhm: Float, rightHwhm: Float): this.type = {
    if (_leftHwhmList == null) _leftHwhmList = new Array[Float](dataPointsCount)
    if (_rightHwhmList == null) _rightHwhmList = new Array[Float](dataPointsCount)

    _mzList(_index) = mz
    _intensityList(_index) = intensity
    _leftHwhmList(_index) = leftHwhm
    _rightHwhmList(_index) = rightHwhm
    _index += 1
    //_capacity += 1
    this
  }

  @inline
  def addData(
    mzList: Seq[Double],
    intensityList: Seq[Float],
    leftHwhmList: Seq[Float],
    rightHwhmList: Seq[Float]
  ): this.type = {
    var nDataPoints = mzList.length
    var i = 0
    if (leftHwhmList != null && rightHwhmList != null) {
      while (i < nDataPoints) {
        this.addDataPoint(mzList(i), intensityList(i), leftHwhmList(i), rightHwhmList(i))
        i += 1
      }
    } else {
      while (i < nDataPoints) {
        this.addDataPoint(mzList(i), intensityList(i))
        i += 1
      }
    }

    /*this._mzList ++= mzList
    this._intensityList ++= intensityList
    if (leftHwhmList != null && rightHwhmList != null) {
      this._leftHwhmList ++= leftHwhmList
      this._rightHwhmList ++= rightHwhmList
    }*/
    this
  }

  @inline
  def addSpectrumData(spectrumData: ISpectrumData): this.type = {
    if (spectrumData != null) {
      this.addData(
        spectrumData.mzList,
        spectrumData.intensityList,
        spectrumData.leftHwhmList,
        spectrumData.rightHwhmList
      )
    }
    this
  }

  def result(): SpectrumData = {
    val sd = if (_index == dataPointsCount) {
      SpectrumData(_mzList, _intensityList, _leftHwhmList, _rightHwhmList)
    } else {

      // Create new arrays
      /*val mzList = new Array[Double](_index)
      val intensityList = new Array[Float](_index)
      val leftHwhmList: Array[Float] = null
      val rightHwhmList: Array[Float] = null

      System.arraycopy(_mzList, 0, mzList, 0, _index)
      System.arraycopy(_intensityList, 0, intensityList, 0, _index)

      if (_leftHwhmList != null) {
        val leftHwhmList = new Array[Float](_index)
        val rightHwhmList = new Array[Float](_index)
        System.arraycopy(_leftHwhmList, 0, leftHwhmList, 0, _index)
        System.arraycopy(_rightHwhmList, 0, rightHwhmList, 0, _index)
      }

      SpectrumData(mzList, intensityList, leftHwhmList, rightHwhmList)*/

      SpectrumData(
        new ResizedArray.ofDouble(_mzList, _index),
        new ResizedArray.ofFloat(_intensityList, _index),
        if (_leftHwhmList == null) null else new ResizedArray.ofFloat(_leftHwhmList, _index),
        if (_rightHwhmList == null) null else new ResizedArray.ofFloat(_rightHwhmList, _index)
      )
    }

    this.clear()

    sd
  }

  def clear(): Unit = {
    this._index = 0
  }

  /*def reset(adjustInitialSize: Boolean = true): Unit = {

    // Adjust the initial size
    if (adjustInitialSize)
      this.initialSize = math.max(this.initialSize,_mzList.length)

    // Create new arrays
    this._mzList = new Array[Double](initialSize)
    this._intensityList = new Array[Float](initialSize)
    this._leftHwhmList = new Array[Float](initialSize)
    this._rightHwhmList = new Array[Float](initialSize)
  }*/
}

/*

class SpectrumDataBuilder(var initialSize: Int, var adjustInitialSize: Boolean = true) extends AbstractSpectrumDataBuilder {

  @inline
  private def _newArrayBuilderOfDouble(size: Int): ArrayBuilder[Double] = {
    val ab = new mutable.ArrayBuilder.ofDouble(); ab.sizeHint(size)
    ab
  }
  @inline
  private def _newArrayBuilderOfFloat(size: Int): ArrayBuilder[Float] = {
    val ab = new mutable.ArrayBuilder.ofFloat(); ab.sizeHint(size)
    ab
  }

  private var _mzList = _newArrayBuilderOfDouble(initialSize)
  private var _intensityList = _newArrayBuilderOfFloat(initialSize)
  private var _leftHwhmList = _newArrayBuilderOfFloat(initialSize)
  private var _rightHwhmList = _newArrayBuilderOfFloat(initialSize)

  _mzList.
  @inline
  def addData(
    mzList: Seq[Double],
    intensityList: Seq[Float],
    leftHwhmList: Seq[Float],
    rightHwhmList: Seq[Float]
  ): this.type = {
    this._mzList ++= mzList
    this._intensityList ++= intensityList
    if (leftHwhmList != null && rightHwhmList != null) {
      this._leftHwhmList ++= leftHwhmList
      this._rightHwhmList ++= rightHwhmList
    }
    this
  }

  @inline
  def addSpectrumData(spectrumData: ISpectrumData): this.type = {
    if (spectrumData != null) {
      this.addData(
        spectrumData.mzList,
        spectrumData.intensityList,
        spectrumData.leftHwhmList,
        spectrumData.rightHwhmList
      )
    }
    this
  }

  def result(): SpectrumData = {
    // Create a new SpectrumData
    val newSpectrumData = SpectrumData(_mzList.result(), _intensityList, _leftHwhmList, _rightHwhmList)

    // Adjust the initial size
    /*if (adjustInitialSize)
      this.initialSize = math.max(this.initialSize,_mzList.length)

    // Create new arrays
    this._mzList = new Array[Double](initialSize)
    this._intensityList = new Array[Float](initialSize)
    this._leftHwhmList = new Array[Float](initialSize)
    this._rightHwhmList = new Array[Float](initialSize)*/

    newSpectrumData
  }
}


 */