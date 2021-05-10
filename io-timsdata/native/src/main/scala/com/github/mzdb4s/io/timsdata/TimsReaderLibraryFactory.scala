package com.github.mzdb4s.io.timsdata

object TimsReaderLibraryFactory {

  def getLibrary() = TimsReaderLibrary

  def initLogger(): Unit = {
    TimsReaderLibrary.timsreader_init_logger()
  }
}