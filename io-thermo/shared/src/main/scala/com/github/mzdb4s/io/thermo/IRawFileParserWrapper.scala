package com.github.mzdb4s.io.thermo

trait IRawFileParserWrapper {

  def isInitialized: Boolean
  def initialize(rawFileParserDirectory: String): Unit
  def getRawFileStreamer(rawFilePath: String): AbstractRawFileStreamer

  protected def getMonoRuntimeAssemblyPath(): String = {
    // MONO_PATH should refer to a fully defined and absolute path to MONO (like "/usr/lib/mono/4.0")
    Option(System.getenv("MONO_PATH")).map( monoPath =>
      monoPath.split("mono").headOption.getOrElse(monoPath)
    ).getOrElse("/usr/lib/")
  }

}
