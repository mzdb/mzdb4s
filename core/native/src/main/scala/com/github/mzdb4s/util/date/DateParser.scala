package com.github.mzdb4s.util.date

import com.github.sqlite4s.c.util.CUtils

import scala.scalanative.native._
import scala.scalanative.posix.time
import scala.scalanative.native.string

// FIXME: use time.strptime when available in the SN library that we use
@extern
private object time_ext {
  def strptime(str: Ptr[CChar], format: CString, time: Ptr[scala.scalanative.posix.time.tm]): CString = extern
}

object DateParser {
  def parseIsoDate(dateStr: String): java.util.Date = {
    val tm_ptr = stackalloc[time.tm]

    string.memset(tm_ptr.asInstanceOf[Ptr[Byte]], 0, sizeof[time.time_t])

    Zone { implicit z =>
      time_ext.strptime(CUtils.toCString(dateStr), c"%Y-%m-%dT%H:%M:%SZ", tm_ptr) //"yyyy-MM-dd'T'HH:mm:ss'Z'"
    }

    val timeSinceEpoch = time.mktime(tm_ptr).asInstanceOf[time.time_t]

    new java.util.Date(timeSinceEpoch * 1000)
  }
}
