package com.github.mzdb4s.io.thermo

import java.io.File
import com.sun.jna._
import mono.embeddinator.Runtime.RuntimeLibrary
import thermorawfileparser.thermorawfileparser._

import java.nio.file.Files

object RawFileParserWrapper extends IRawFileParserWrapper with com.github.mzdb4s.Logging {

  private lazy val TEMP_DIR = {
    val dir = Files.createTempDirectory("mzdb4s-io-thermo-").toFile
    dir.deleteOnExit()
    dir
  }

  //new File(System.getProperty("java.io.tmpdir"),"mzdb4s-io-thermo")
  //TEMP_DIR.mkdir()

  private var _isInitialized = false

  def isInitialized: Boolean = _isInitialized

  /*private trait MonoEmbeddinatorInterface extends com.sun.jna.Library {
    def mono_embeddinator_set_assembly_path(path: String): Unit
    def mono_embeddinator_set_runtime_assembly_path(path: String): Unit
    def mono_embeddinator_install_error_report_hook(cb: RuntimeLibrary.ErrorCallback): Pointer
    //def mono_object_to_string(obj: Pointer, ex: Pointer): Pointer
    //def mono_string_length(str: Pointer): Int
    def mono_embeddinator_error_to_string(err: RuntimeLibrary.Error.ByValue): String
  }*/

  def initialize(rawFileParserDirectory: String): Unit = {
    require(
      new File(rawFileParserDirectory, "ThermoRawFileParser.dll").isFile,
      s"can't find file 'ThermoRawFileParser.dll' in directory '$rawFileParserDirectory'"
    )

    assert(!_isInitialized, "wrapper already initialized")
    assert(!mono.embeddinator.Runtime.initialized, "Runtime already initialized")

    // Extract/retrieve the Mono embeddinator JNA library
    val wrapperLibFile = RawFileParserEnv.getLibraryFile().getOrElse(
      RawFileParserEnv.extractLibrary(None)
    )

    // Hack the embeddinator.Runtime class to manage the initialization here and bypass the native libraries extraction/loading
    System.setProperty("jna.encoding", "utf8")
    System.setProperty("jna.library.path", wrapperLibFile.getParentFile.getCanonicalFile.getAbsolutePath)

    val runtimeLibrary = Native.loadLibrary(RawFileParserEnv.libraryFileName, classOf[RuntimeLibrary])
    mono.embeddinator.Runtime.setImplementation(new mono.embeddinator.DesktopImpl())
    mono.embeddinator.Runtime.runtimeLibrary = runtimeLibrary
    mono.embeddinator.Runtime.initialized = true

    // Was used before alternatively
    //mono.embeddinator.Runtime.initialize("ThermoRawFileParser")
    //val runtimeLibrary = mono.embeddinator.Runtime.runtimeLibrary

    // --- Configure the Mono embeddinator ---
    // FIXME: create issue on E4K github (backslash and/or missing last slash fails mono loading)
    val rawFileParserAbsDir = new File(rawFileParserDirectory).getCanonicalFile.getAbsolutePath.replace('\\','/') + "/"

    // C# DLL location
    runtimeLibrary.mono_embeddinator_set_assembly_path(rawFileParserAbsDir)

    // Mono runtime location
    if (Platform.isWindows) {
      runtimeLibrary.mono_embeddinator_set_runtime_assembly_path(rawFileParserAbsDir)
    } else {
      runtimeLibrary.mono_embeddinator_set_runtime_assembly_path("/usr/lib/")
    }

    // Replace the RuntimeLibrary.ErrorCallback by a custom version to retrieve the C# exception stack traces
    // FIXME: perform this fix directly in Embeddinator (submit a PR)
    val errCb = new RuntimeLibrary.ErrorCallback() {
      def invoke(error: RuntimeLibrary.Error.ByValue): Unit = {
        if (error.`type` == RuntimeLibrary.ErrorType.MONO_EMBEDDINATOR_OK)
          return

        logger.error("uncaught error in Mono RuntimeLibrary:" + error.`type`)
        throw new Exception("Mono has crashed!")

        /*if (error.monoException != null) {
          throw new Exception(runtimeLibraryLite.mono_embeddinator_error_to_string(error))
        }*/
      }
    }

    runtimeLibrary.mono_embeddinator_install_error_report_hook(errCb)

    // Show the Thermo RawFileParser version
    val libVersion = MainClass.getVersion()
    logger.info(s"ThermoRawFileParser version '$libVersion' has been successfully loaded!")

    _isInitialized = true
  }

  def getRawFileStreamer(rawFilePath: String): RawFileStreamer = {
    require(_isInitialized, "wrapper must be initialized first")
    new RawFileStreamer(new File(rawFilePath))
  }

  private def _extractLibrary(resourceName: String): File = {
    val cl = Thread.currentThread.getContextClassLoader

    val fileURL = cl.getResource(resourceName)
    if (fileURL != null) {
      if (fileURL.toExternalForm.startsWith("file:")) {
        val f = new File(fileURL.toURI)
        if (f.isFile) return f
      }
    }

    val tempDir = TEMP_DIR
    assert(tempDir.exists() && tempDir.isDirectory,"Invalid extraction directory " + tempDir)

    val outputFile = new File(TEMP_DIR,new File(resourceName).getName)
    if (outputFile.exists()) return outputFile

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

