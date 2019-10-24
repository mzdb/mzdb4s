package com.github.mzdb4s.msdata

import java.nio.ByteOrder

import scala.beans.BeanProperty

trait IDataEncoding extends Any {
  def getId(): Int
  def getMode(): DataMode.Value
  def getPeakEncoding(): PeakEncoding.Value
  def getCompression(): String
  def getByteOrder(): ByteOrder
  def getPeakStructSize(): Int
}

case class DataEncoding(
  @BeanProperty id: Int,
  @BeanProperty mode: DataMode.Value,
  @BeanProperty var peakEncoding: PeakEncoding.Value,
  @BeanProperty compression: String,
  @BeanProperty byteOrder: ByteOrder
) extends IDataEncoding {
  private var _peakStructSize: Int = this.getPeakEncoding.id
  if (this.getMode == DataMode.FITTED) _peakStructSize += 8 // add 2 floats (left hwhm and right hwhm)

  def peakStructSize: Int = _peakStructSize
  def getPeakStructSize(): Int = _peakStructSize
}