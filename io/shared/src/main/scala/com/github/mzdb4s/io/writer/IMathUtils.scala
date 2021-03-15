package com.github.mzdb4s.io.writer

trait IMathUtils {

  def round(num: Double, decimalPlaces: Int): String
  def round(num: Float, decimalPlaces: Int): String

}
