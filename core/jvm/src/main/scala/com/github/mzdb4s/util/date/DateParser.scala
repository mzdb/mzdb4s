package com.github.mzdb4s.util.date

object DateParser {
  import java.text.SimpleDateFormat
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  @inline
  def parseIsoDate(dateStr: String): java.util.Date = dateFormat.parse(dateStr)
}
