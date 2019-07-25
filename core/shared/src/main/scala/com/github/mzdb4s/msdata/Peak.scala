package com.github.mzdb4s.msdata

import java.util.Comparator

import scala.beans.BeanProperty

object Peak {
  def getIntensityComp(): Comparator[Peak] = intensityComp

  /** The intensity comparator */
  private var intensityComp: Comparator[Peak] = new Comparator[Peak]() { // @Override
    override def compare(peak0: Peak, peak1: Peak): Int = if (peak0.intensity < peak1.intensity) -1
    else if (Math.abs(peak0.intensity - peak1.intensity) < 1e-6) 0
    else 1
  }
}

case class Peak(
  @BeanProperty mz: Double,
  @BeanProperty var intensity: Float,
  @BeanProperty leftHwhm: Float,
  @BeanProperty rightHwhm: Float,
  @BeanProperty var lcContext: ILcContext
) extends Comparable[Peak] {

  //private var _nf = -1

  def this(mz: Double, intensity: Float) {
    this(mz, intensity, 0, 0, null)
  }

  @throws[ClassCastException]
  def getSpectrumHeader(): SpectrumHeader = lcContext.asInstanceOf[SpectrumHeader]

  /*def isNormalized: Boolean = this._nf > 0

  def normalizeIntensity(nf: Float): Unit = {
    if (nf < 0) throw new IllegalArgumentException("nf can't be negative")
    if (this._nf > 0) throw new Exception("peak intensity has been already normalized")
    this._nf = nf
    this.intensity *= nf
  }*/

  override def compareTo(aPeak: Peak): Int = {
    if (mz < aPeak.mz) -1
    else if (Math.abs(mz - aPeak.mz) < Double.MinValue) 0
    else 1
  }
}