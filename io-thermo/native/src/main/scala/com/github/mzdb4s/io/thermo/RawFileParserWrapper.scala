package com.github.mzdb4s.io.thermo

import java.io.File

object RawFileParserWrapper extends com.github.mzdb4s.Logging {

  def getRawFileStreamer(rawFilePath: String): RawFileStreamer = new RawFileStreamer(new File(rawFilePath))

}
