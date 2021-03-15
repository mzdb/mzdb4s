package com.github.mzdb4s.io.thermo

import java.io.File
import java.nio.file.Files

import com.sun.jna.Platform

object RawFileParserEnv {

  //private var _libExtractDir = Option.empty[File]
  private var _libFile = Option.empty[File]
  private var _isLibExtracted = false

  /*def getLibExtractionDirectory(): File = {
    _libExtractDir.getOrElse {
      val dir = Files.createTempDirectory("mzdb4s-io-thermo-").toFile
      dir.deleteOnExit()
      dir
    }
  }

  def setLibExtractionDirectory(libraryDir: File): Unit = {
    require(libraryDir.isDirectory, "directory doesn't exist at: $libraryDir")
    _libExtractDir = Some(libraryDir)
  }*/

  def getLibraryFile(): Option[File] = _libFile

  def setLibraryFile(libraryFile: File): Unit = {
    require(libraryFile.isFile, s"library file doesn't exist at: $libraryFile")
    _libFile = Some(libraryFile)
  }

  val libraryFileName = {
    if (Platform.isWindows) {
      "ThermoRawFileParser.dll"
    } else if (Platform.isLinux) {
      "libThermoRawFileParser.so"
    } else {
      throw new Exception("The current operating system is not supported")
    }
  }

  def extractLibrary(extractionDir: Option[File] = None): File = {
    require(!_isLibExtracted," library already extracted")

    val outputDir = extractionDir.getOrElse {
      val dir = Files.createTempDirectory("mzdb4s-io-thermo-").toFile
      dir.deleteOnExit()
      dir
    }
    require(outputDir.exists() && outputDir.isDirectory, s"Invalid extraction directory $outputDir")

    val isTempDir = extractionDir.isEmpty

    val wrapperLibFile = if (Platform.isWindows) {
      _extractLibrary("windows/mono-2.0-sgen.dll", outputDir, isTempDir)
      _extractLibrary("windows/ThermoRawFileParser.dll", outputDir, isTempDir)
    } else if (Platform.isLinux) {
      _extractLibrary("linux/libmonosgen-2.0.so", outputDir, isTempDir)
      _extractLibrary("linux/libThermoRawFileParser.so", outputDir, isTempDir)
    } else {
      throw new Exception("The current operating system is not supported")
    }

    _libFile = Some(wrapperLibFile)
    _isLibExtracted = true

    wrapperLibFile
  }

  private def _extractLibrary(resourceName: String, outputDir: File, useCachedResource: Boolean): File = {
    val cl = Thread.currentThread.getContextClassLoader

    if (useCachedResource) {
      val fileURL = cl.getResource(resourceName)
      if (fileURL != null) {
        if (fileURL.toExternalForm.startsWith("file:")) {
          val f = new File(fileURL.toURI)
          if (f.isFile) return f
        }
      }
    }

    val outputFile = new File(outputDir,new File(resourceName).getName)
    assert(!outputFile.exists(), s"library file already exists: $outputFile")

    val in = cl.getResourceAsStream(resourceName)
    val out = Files.newOutputStream(outputFile.toPath)

    try {
      require(in != null, s"Classpath resource '$resourceName' not found")

      val buffer = new Array[Byte](4096)
      var readLength = in.read(buffer)
      while (readLength != -1) {
        out.write(buffer, 0, readLength)
        readLength = in.read(buffer)
      } // End of copy loop

    } finally {
      if (in != null) in.close()
      if (out != null) out.close()
    }

    outputFile
  }
}