package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

trait ISpectrum {
  def header: SpectrumHeader
  def data: ISpectrumData

  def toPeaks(): Array[Peak]

  def getNearestPeak(mz: Double, mzTolPPM: Double): Peak
}

abstract class AbstractSpectrum extends ISpectrum {
  def toPeaks(): Array[Peak] = data.toPeaks(this.header)

  def getNearestPeak(mz: Double, mzTolPPM: Double): Peak = this.data.getNearestPeak(mz, mzTolPPM, header)
}

case class Spectrum(
  @BeanProperty header: SpectrumHeader,
  @BeanProperty data: ISpectrumData
) extends AbstractSpectrum {
  require(header != null, "a SpectrumHeader must be provided")
  require(data != null, "a SpectrumData must be provided")
}