package com.github.mzdb4s.io.timsdata

import java.text.SimpleDateFormat
import java.util.Calendar

object DateParser extends IDateParser {

  private val _sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

  // Example: 2018-08-21T20:40:14.356+02:00
  def parseTDFDate(dateStr: String): java.util.Date = {
    if (dateStr == null) return null

    try _sdf.parse(dateStr)
    catch {
      case e: Exception  =>
        Calendar.getInstance.getTime
    }
  }

}
