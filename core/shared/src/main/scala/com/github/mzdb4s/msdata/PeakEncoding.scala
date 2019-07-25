package com.github.mzdb4s.msdata

object PeakEncoding extends Enumeration {
  val HIGH_RES_PEAK: Value = Value(12)
  val LOW_RES_PEAK: Value = Value(8)
  val NO_LOSS_PEAK: Value = Value(16)
}