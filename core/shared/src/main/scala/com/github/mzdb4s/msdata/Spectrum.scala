package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

trait ISpectrum {
  def header: SpectrumHeader
  def data: ISpectrumData

  def forEachPeak(peakConsumer: (IPeak, Int) => Unit): Unit
  def toPeaks(): Array[IPeak]

  def getNearestPeak(mz: Double, mzTolPPM: Double): IPeak
}

abstract class AbstractSpectrum extends ISpectrum {
  def forEachPeak(peakConsumer: (IPeak, Int) => Unit): Unit = {
    data.forEachPeak(this.header)(peakConsumer)
  }
  def toPeaks(): Array[IPeak] = data.toPeaks(this.header)
  def getNearestPeak(mz: Double, mzTolPPM: Double): IPeak = this.data.getNearestPeak(mz, mzTolPPM, header)
}

case class Spectrum(
  @BeanProperty header: SpectrumHeader,
  @BeanProperty data: ISpectrumData
  //@BeanProperty dataEncoding: Option[DataEncoding] = None
) extends AbstractSpectrum {
  require(header != null, "a SpectrumHeader must be provided")
  require(data != null, "a SpectrumData must be provided")
}