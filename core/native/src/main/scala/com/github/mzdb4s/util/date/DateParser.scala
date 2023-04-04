package com.github.mzdb4s.util.date

import scala.scalanative.unsafe._
import scala.scalanative.posix.time
import scala.scalanative.libc.string
import scala.scalanative.unsigned.UnsignedRichInt

/*
// Note: was used when time.strptime was not available in the SN library
@extern
private object time_ext {
  def strptime(str: Ptr[CChar], format: CString, time: Ptr[scala.scalanative.posix.time.tm]): CString = extern
}*/

/*
object timeOps {
  import time.{time_t, timespec, tm}

  implicit class timespecOps(val ptr: Ptr[timespec]) extends AnyVal {
    def tv_sec: time_t            = ptr._1
    def tv_nsec: CLong            = ptr._2
    def tv_sec_=(v: time_t): Unit = ptr._1 = v
    def tv_nsec_=(v: CLong): Unit = ptr._2 = v
  }

  implicit class tmOps(val ptr: Ptr[tm]) extends AnyVal {
    def tm_sec: CInt              = ptr._1
    def tm_min: CInt              = ptr._2
    def tm_hour: CInt             = ptr._3
    def tm_mday: CInt             = ptr._4
    def tm_mon: CInt              = ptr._5
    def tm_year: CInt             = ptr._6
    def tm_wday: CInt             = ptr._7
    def tm_yday: CInt             = ptr._8
    def tm_isdst: CInt            = ptr._9
    def tm_sec_=(v: CInt): Unit   = ptr._1 = v
    def tm_min_=(v: CInt): Unit   = ptr._2 = v
    def tm_hour_=(v: CInt): Unit  = ptr._3 = v
    def tm_mday_=(v: CInt): Unit  = ptr._4 = v
    def tm_mon_=(v: CInt): Unit   = ptr._5 = v
    def tm_year_=(v: CInt): Unit  = ptr._6 = v
    def tm_wday_=(v: CInt): Unit  = ptr._7 = v
    def tm_yday_=(v: CInt): Unit  = ptr._8 = v
    def tm_isdst_=(v: CInt): Unit = ptr._9 = v
  }
}*/

object DateParser {

  def parseIsoDate(dateStr: String): java.util.Date = {

    val tm_ptr = stackalloc[time.tm]()

    string.memset(tm_ptr.asInstanceOf[Ptr[Byte]], 0, sizeof[time.time_t])

    val timeSinceEpoch = scala.scalanative.unsafe.Zone { implicit z =>
      // FIXME: calling CUtils.toCString doesn't work anymore in SN v0.4
      //time_ext.strptime(CUtils.toCString(dateStr), c"%Y-%m-%dT%H:%M:%SZ", tm_ptr) //"yyyy-MM-dd'T'HH:mm:ss'Z'"

      val isoDateAsCStr = toCString(dateStr)(z)
      val format = c"%Y-%m-%dT%H:%M:%SZ" //"yyyy-MM-dd'T'HH:mm:ss'Z'"

      // Workaround for missing support of strptime on Windows
      if (scalanative.meta.LinktimeInfo.isWindows) {
        com.github.utils4sn.bindings.StrptimeLib.strptime(isoDateAsCStr, format, tm_ptr)
        time.mktime(tm_ptr)
      } else {
        time.strptime(isoDateAsCStr, format, tm_ptr)
        time.mktime(tm_ptr)
      }
    }

    new java.util.Date(timeSinceEpoch * 1000)
  }

  final private val DATE_SIZE = 70.toULong

  def dateToIsoString(date: java.util.Date): String = {

    Zone { implicit z =>
      val time_ptr = alloc[time.time_t]()
      time_ptr.update(0.toUInt, date.getTime / 1000)

      val timePtr                = time.localtime(time_ptr)
      val isoDatePtr: Ptr[CChar] = alloc[CChar](DATE_SIZE)

      time.strftime(isoDatePtr, DATE_SIZE, c"%FT%TZ", timePtr)

      fromCString(isoDatePtr)
    }
  }
}
