package com.github.mzdb4s.io.writer

import scala.scalanative.unsafe._

object MathUtils extends IMathUtils {
  @inline def round(num: Double, decimalPlaces: Int): String = {
    val cstr = stackalloc[CChar](24)
    com.github.utils4sn.bindings.StrBuilderLib.dtoa(num, cstr, decimalPlaces)
    fromCString(cstr)
  }

  @inline def round(num: Float, decimalPlaces: Int): String = {
    val cstr = stackalloc[CChar](24)
    com.github.utils4sn.bindings.StrBuilderLib.ftoa(num, cstr, decimalPlaces)
    fromCString(cstr)
  }
}
