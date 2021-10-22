package com.github.utils4sn.bindings

import scala.scalanative.posix.time
import scala.scalanative.unsafe._

@extern
@link("strptime")
object StrptimeLib {
  def strptime(s: CString, format: CString, tm: Ptr[time.tm]): CString = extern
}