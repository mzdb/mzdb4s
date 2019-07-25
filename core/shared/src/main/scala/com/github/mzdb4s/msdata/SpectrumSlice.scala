package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

case class SpectrumSlice(
  @BeanProperty header: SpectrumHeader,
  @BeanProperty data: ISpectrumData
) extends AbstractSpectrum with ISpectrum {
  require(header != null, "a SpectrumHeader must be provided")
  require(data != null, "a SpectrumData must be provided")

  protected var runSliceId = 0

  def getSpectrumId(): Long = header.getId

  def getRunSliceId(): Int = runSliceId
  def setRunSliceId(runSliceId: Int): Unit = this.runSliceId = runSliceId

}