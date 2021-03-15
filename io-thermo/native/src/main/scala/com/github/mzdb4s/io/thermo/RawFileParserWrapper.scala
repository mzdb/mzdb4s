package com.github.mzdb4s.io.thermo

import java.io.File

import scala.scalanative.unsafe._

import com.github.sqlite4s.c.util.CUtils

object RawFileParserWrapper extends IRawFileParserWrapper with com.github.mzdb4s.Logging {

  private var _isInitialized = false

  def isInitialized: Boolean = _isInitialized

  def initialize(rawFileParserDirectory: String): Unit = {
    require(!_isInitialized, "wrapper already initialized")

    require(
      new File(rawFileParserDirectory, "ThermoRawFileParser.dll").isFile,
      s"can't find file 'ThermoRawFileParser.dll' in directory '$rawFileParserDirectory'"
    )

    // --- Configure the Mono embeddinator ---
    // FIXME: create issue on E4K github (backslash and/or missing last slash fails mono loading)
    val rawFileParserAbsDir = new File(rawFileParserDirectory).getCanonicalFile.getAbsolutePath.replace('\\', '/') + "/"

    Zone { implicit z =>
      // Mono runtime location
      bindings.MonoEmbeddinator.mono_embeddinator_set_runtime_assembly_path(c"/usr/lib/")
      // C# DLL location
      bindings.MonoEmbeddinator.mono_embeddinator_set_assembly_path(CUtils.toCString(rawFileParserAbsDir))
    }

    _isInitialized = true
  }

  def getRawFileStreamer(rawFilePath: String): RawFileStreamer = {
    require(_isInitialized, "wrapper must be initialized first")
    new RawFileStreamer(new File(rawFilePath))
  }
}

