package com.github.mzdb4s.io.timsdata

import scala.scalanative.unsafe._
import scala.scalanative.posix.time
import scala.scalanative.libc.string

/*
// Note: used before time.strptime was available in the SN library
@extern
private object time_ext {
  def strptime(str: Ptr[CChar], format: CString, time: Ptr[scala.scalanative.posix.time.tm]): CString = extern
}*/

object DateParser extends IDateParser {

  // Example: 2018-08-21T20:40:14.356+02:00
  def parseTDFDate(dateStr: String): java.util.Date = {
    if (dateStr == null) return null

    val Array(isoDate,dateSuffix) = dateStr.split('.')
    val Array(millis,zoneHoursShift,zonneMinutesShift) = dateSuffix.split("[+:]")

    val tm_ptr = stackalloc[time.tm]()
    string.memset(tm_ptr.asInstanceOf[Ptr[Byte]], 0, sizeof[time.time_t])

    val timeSinceEpoch = scala.scalanative.unsafe.Zone { implicit z =>
      val isoDateAsCStr = toCString(isoDate)(z)
      // Note: the format is a bit different from com.github.mzdb4s.util.date.DateParser because TimsData date is not ISO8601
      val format = c"%Y-%m-%dT%H:%M:%S" //"yyyy-MM-dd'T'HH:mm:ss'"

      // Workaround for missing support of strptime on Windows
      if (scalanative.meta.LinktimeInfo.isWindows) {
        com.github.utils4sn.bindings.StrptimeLib.strptime(isoDateAsCStr, format, tm_ptr)
        val zoneTimeShiftSecs = (1 + zoneHoursShift.toInt) * 3600 + zonneMinutesShift.toInt * 60
        time.mktime(tm_ptr) - zoneTimeShiftSecs
      } else {
        time.strptime(isoDateAsCStr, format, tm_ptr)
        val zoneTimeShiftSecs = zoneHoursShift.toInt * 3600 + zonneMinutesShift.toInt * 60
        time.mktime(tm_ptr) - zoneTimeShiftSecs
      }
    }

    new java.util.Date(timeSinceEpoch * 1000 + millis.toInt)
  }

}
