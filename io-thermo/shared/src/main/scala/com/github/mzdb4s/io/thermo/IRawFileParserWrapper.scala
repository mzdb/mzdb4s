package com.github.mzdb4s.io.thermo

trait IRawFileParserWrapper {

  def isInitialized: Boolean
  def initialize(rawFileParserDirectory: String): Unit
  def getRawFileStreamer(rawFilePath: String): AbstractRawFileStreamer
}
