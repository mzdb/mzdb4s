package com.github.mzdb4s.msdata

import java.nio.ByteOrder

import scala.beans.BeanProperty

case class DataEncoding(
  @BeanProperty id: Int,
  @BeanProperty mode: DataMode.Value,
  @BeanProperty var peakEncoding: PeakEncoding.Value,
  @BeanProperty compression: String,
  @BeanProperty byteOrder: ByteOrder
) {
  private var _peakStructSize: Int = this.getPeakEncoding.id
  if (this.getMode == DataMode.FITTED) _peakStructSize += 8 // add 2 floats (left hwhm and right hwhm)

  def getPeakStructSize: Int = _peakStructSize
}