package com.github.mzdb4s.io.writer

object MathUtils extends IMathUtils {
  def round(num: Double, decimalPlaces: Int): String = {
    if (decimalPlaces == 0) return java.lang.Math.round(num).toString

    val scale = decimalPlaces match {
      case 1 => 10
      case 2 => 100
      case 3 => 1000
      case 4 => 10000
      case _ => math.pow(10,decimalPlaces)
    }

    (java.lang.Math.round(num * scale).toDouble / scale).toString
  }

  def round(num: Float, decimalPlaces: Int): String = {
    if (decimalPlaces == 0) return java.lang.Math.round(num).toString

    val scale = decimalPlaces match {
      case 1 => 10
      case 2 => 100
      case 3 => 1000
      case 4 => 10000
      case _ => math.pow(10,decimalPlaces)
    }

    (java.lang.Math.round(num * scale).toDouble / scale).toString
  }
}
