package com.github.mzdb4s.msdata.builder

import com.github.mzdb4s.msdata._

/*class SpectrumSliceBuilder(val header: SpectrumHeader, val initialDataSize: Int) {

  private val spectrumDataBuilder = new SpectrumDataBuilder(initialDataSize)

  def addData(
    mzList: Array[Double],
    intensityList: Array[Float],
    leftHwhmList: Array[Float],
    rightHwhmList: Array[Float]
  ): SpectrumSliceBuilder = {
    spectrumDataBuilder.addData(mzList, intensityList, leftHwhmList, rightHwhmList)
    this
  }

  def addSpectrumData(spectrumData: SpectrumData): SpectrumSliceBuilder = {
    spectrumDataBuilder.addSpectrumData(spectrumData)
    this
  }

  def result(): SpectrumSlice = SpectrumSlice(header, spectrumDataBuilder.result())
}
*/

class SpectrumSliceBuilder(val header: SpectrumHeader, val spectrumDataBuilder: ISpectrumDataBuilder) extends AbstractSpectrumBuilder {

  def this(header: SpectrumHeader, initialDataSize: Int) {
    this(header, new SpectrumDataBuilder(initialDataSize))
  }

  def result(): SpectrumSlice = SpectrumSlice(header, spectrumDataBuilder.result())
}