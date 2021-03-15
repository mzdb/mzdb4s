package com.github.mzdb4s.util.date

import java.text.SimpleDateFormat
import java.util.{Locale, TimeZone}

object DateParser {

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

  @inline
  def parseIsoDate(dateStr: String): java.util.Date = dateFormat.parse(dateStr)

  @inline
  def dateToIsoString(date: java.util.Date): String = dateFormat.format(date)
}
