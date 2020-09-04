package com.github.mzdb4s.io.thermo

import java.io.File

import com.sun.jna._
import mono.embeddinator.Runtime.RuntimeLibrary
import thermorawfileparser.thermorawfileparser._

object RawFileParserWrapper extends com.github.mzdb4s.Logging {

  private trait RuntimeLibraryLite extends com.sun.jna.Library {
    def mono_embeddinator_set_assembly_path(path: String): Unit
    def mono_embeddinator_set_runtime_assembly_path(path: String): Unit
    def mono_embeddinator_install_error_report_hook(cb: mono.embeddinator.Runtime.RuntimeLibrary.ErrorCallback): Pointer
    //def mono_object_to_string(obj: Pointer, ex: Pointer): Pointer
    //def mono_string_length(str: Pointer): Int
    def mono_embeddinator_error_to_string(err: RuntimeLibrary.Error.ByValue): String
  }

  def initialize(rawFileParserDirectory: String, nativeWrapperDirectory: String): Unit = {

    require(
      new File(rawFileParserDirectory, "ThermoRawFileParser.dll").isFile(),
      s"can't find file 'ThermoRawFileParser.dll' in directory '$rawFileParserDirectory'"
    )

    var libraryFileName: String = null
    var runtimeFileName: String = null

    if (Platform.isWindows) {
      libraryFileName = "ThermoRawFileParser.dll"
      runtimeFileName = "mono-2.0-sgen.dll"
    } else {
      libraryFileName = "libThermoRawFileParser.so"
      runtimeFileName = "libmonosgen-2.0.so"
    }
    // \wsl\scala-native\mzdb4s\io-thermo\shared\src\main\resources\rawfileparser\mono\4.5\mscorlib.dll
    // /wsl/scala-native/mzdb4s/io-thermo/shared/src/main/resources/mono/4.5/mscorlib.dll

    val jnaDir = new File(nativeWrapperDirectory)

    require(
      new File(jnaDir, libraryFileName).isFile(),
      s"can't find file '$libraryFileName' in directory '$nativeWrapperDirectory'"
    )
    /*require(
      new File(jnaDir, runtimeFileName).isFile(),
      s"can't find file '$runtimeFileName' in directory '$nativeWrapperDirectory'"
    )*/

    System.setProperty("jna.encoding", "utf8")
    System.setProperty("jna.library.path", jnaDir.getCanonicalFile.getAbsolutePath)

    val runtimeLibraryLite = Native.loadLibrary(libraryFileName, classOf[RuntimeLibraryLite])

    // Load the Mono runtime
    mono.embeddinator.Runtime.initialize("ThermoRawFileParser")

    // Hack the embeddinator.Runtime class to manage the initialization here and bypass the native libraries extraction/loading
    val runtimeLibrary = mono.embeddinator.Runtime.runtimeLibrary
    assert(!mono.embeddinator.Runtime.initialized, "Runtime already initialized")
    mono.embeddinator.Runtime.initialized = true

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

        println("error is:" + error.`type`)
        throw new Exception("Mono is crashing")

        /*if (error.monoException != null) {
          throw new Exception(runtimeLibraryLite.mono_embeddinator_error_to_string(error))
        }*/
      }
    }

    runtimeLibrary.mono_embeddinator_install_error_report_hook(errCb)

    // Show the Thermo RawFileParser version
    val libVersion = MainClass.getVersion()
    logger.info(s"ThermoRawFileParser version '$libVersion' has been successfully loaded")
  }

  def getRawFileStreamer(rawFilePath: String): RawFileStreamer = new RawFileStreamer(new File(rawFilePath))


}
