package com.github.mzdb4s.io.timsdata

object DateParserFactory {

  def getInstance(): IDateParser = {
    com.github.mzdb4s.io.timsdata.DateParser
  }

}

trait IDateParser {
  def parseTDFDate(dateStr: String): java.util.Date
}
