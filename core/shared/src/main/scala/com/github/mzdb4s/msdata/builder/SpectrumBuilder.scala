package com.github.mzdb4s.msdata.builder

import com.github.mzdb4s.msdata._

trait ISpectrumBuilder extends ISpectrumDataAdder {
  def spectrumDataBuilder: ISpectrumDataBuilder
}

abstract class AbstractSpectrumBuilder extends ISpectrumBuilder {

  @inline
  def addDataPoint(mz: Double, intensity: Float): this.type = {
    spectrumDataBuilder.addDataPoint(mz, intensity)
    this
  }

  @inline
  def addDataPoint(mz: Double, intensity: Float, leftHwhm: Float, rightHwhm: Float): this.type = {
    spectrumDataBuilder.addDataPoint(mz, intensity, leftHwhm, rightHwhm)
    this
  }

  @inline
  def addData(
    mzList: Seq[Double],
    intensityList: Seq[Float],
    leftHwhmList: Seq[Float],
    rightHwhmList: Seq[Float]
  ): this.type = {
    spectrumDataBuilder.addData(mzList, intensityList, leftHwhmList, rightHwhmList)
    this
  }

  @inline
  def addSpectrumData(spectrumData: ISpectrumData): this.type = {
    spectrumDataBuilder.addSpectrumData(spectrumData)
    this
  }

}


class SpectrumBuilder(val header: SpectrumHeader, val spectrumDataBuilder: SpectrumDataBuilder) extends AbstractSpectrumBuilder {

  def this(header: SpectrumHeader, initialDataSize: Int) {
    this(header, new SpectrumDataBuilder(initialDataSize))
  }

  def result(): Spectrum = Spectrum(header, spectrumDataBuilder.result())
}