package com.github.mzdb4s.msdata

import java.util.Comparator

import scala.beans.BeanProperty

trait IPeak extends Any {
  def getMz(): Double
  def getIntensity(): Float
  def getLeftHwhm(): Float
  def getRightHwhm(): Float
  def getLcContext(): ILcContext
}

object Peak {
  def getIntensityComp(): Comparator[IPeak] = _intensityComp
  def getIntensityOrdering(): Ordering[IPeak] = _intensityOrd

  /** The intensity comparator */
  private var _intensityComp: Comparator[IPeak] = new Comparator[IPeak]() { // @Override
    override def compare(peak0: IPeak, peak1: IPeak): Int = if (peak0.getIntensity() < peak1.getIntensity) -1
    else if (Math.abs(peak0.getIntensity - peak1.getIntensity) < 1e-6) 0
    else 1
  }

  private lazy val _intensityOrd = scala.math.Ordering.comparatorToOrdering(_intensityComp)
}

case class Peak(
  @BeanProperty mz: Double,
  @BeanProperty var intensity: Float,
  @BeanProperty leftHwhm: Float, // 0f if undefined
  @BeanProperty rightHwhm: Float, // 0f if undefined
  @BeanProperty var lcContext: ILcContext
) extends IPeak with Comparable[IPeak] {

  //private var _nf = -1

  def this(mz: Double, intensity: Float) {
    this(mz, intensity, 0f, 0f, null)
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

  override def compareTo(aPeak: IPeak): Int = {
    if (mz < aPeak.getMz) -1
    else if (Math.abs(mz - aPeak.getMz) < Double.MinValue) 0
    else 1
  }
}