package com.github.mzdb4s.io.timsdata

import scala.scalanative.unsafe._
import scala.scalanative.posix.time
import scala.scalanative.libc.string

// FIXME: use time.strptime when available in the SN library that we use
@extern
private object time_ext {
  def strptime(str: Ptr[CChar], format: CString, time: Ptr[scala.scalanative.posix.time.tm]): CString = extern
}

object DateParser extends IDateParser {

  // Example: 2018-08-21T20:40:14.356+02:00
  def parseTDFDate(dateStr: String): java.util.Date = {
    if (dateStr == null) return null

    val tm_ptr = stackalloc[time.tm]

    val Array(isoDate,dateSuffix) = dateStr.split('.')
    val Array(millis,zoneHoursShift,zonneMinutesShift) = dateSuffix.split("[+:]")

    string.memset(tm_ptr.asInstanceOf[Ptr[Byte]], 0, sizeof[time.time_t])

    scala.scalanative.unsafe.Zone { implicit z =>
      time_ext.strptime(toCString(isoDate)(z), c"%Y-%m-%dT%H:%M:%S", tm_ptr) //"yyyy-MM-dd'T'HH:mm:ss'"
    }

    val zonTimeShiftSecs = zoneHoursShift.toInt * 3600 + zonneMinutesShift.toInt * 60
    val timeSinceEpoch = time.mktime(tm_ptr) + zonTimeShiftSecs

    new java.util.Date(timeSinceEpoch * 1000 + millis.toInt)
  }

}
