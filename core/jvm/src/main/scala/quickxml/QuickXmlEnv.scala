package quickxml

import java.io.File
import java.nio.file.Files

object QuickXmlEnv {

  private val TEMP_DIR = System.getProperty("java.io.tmpdir")

  private lazy val _classLoader = Thread.currentThread.getContextClassLoader
  //private var _libExtractDir = Option.empty[String]
  private var _libPath = Option.empty[String]
  private var _isLibExtracted = false

  /*def getLibExtractionDirectory(): Option[String] = _libExtractDir
  def setLibExtractionDirectory(libraryDir: String): Unit = {
    _libExtractDir = Some(libraryDir)
  }*/

  def getLibraryPath(): Option[String] = _libPath
  def setLibraryPath(libraryPath: String): Unit = {
    _libPath = Some(libraryPath)
  }

  val libraryFileName = {
    val os = jnr.ffi.Platform.getNativePlatform.getOS
    if (os == jnr.ffi.Platform.OS.WINDOWS) {
      "quickxml.dll"
    } else if (os == jnr.ffi.Platform.OS.LINUX) {
      "libquickxml.so"
    } else {
      throw new Exception("The current operating system is not supported")
    }
  }

  def extractLibrary(extractionDir: Option[File] = None): File = {
    require(!_isLibExtracted," library already extracted")
    val outputDir = extractionDir.getOrElse(new File(TEMP_DIR))
    require(outputDir.exists() && outputDir.isDirectory, s"Invalid extraction directory $outputDir")

    val libFileName = QuickXmlEnv.libraryFileName
    val outputFile = if (extractionDir.isDefined) {
      new File(outputDir, libFileName)
    } else {

      val fileURL = _classLoader.getResource(libFileName)
      if (fileURL != null) {
        if (fileURL.toExternalForm.startsWith("file:")) {
          val f = new File(fileURL.toURI)
          if (f.isFile) {
            _isLibExtracted = true
            return f
          }
        }
      }

      val suffix = libFileName.substring(libFileName.lastIndexOf('.'))
      val file = File.createTempFile("mzdb4s-quickml-", suffix, outputDir)
      // FIXME: file.deleteOnExit() should work but doesn't apparrently
      file.deleteOnExit()
      /*Runtime.getRuntime().addShutdownHook(new Thread() {
        override def run(): Unit = {
          rustLib = null
          System.gc()
          println(s"try to delete file $file: " +  file.delete())
        }
      })*/

      file
    }

    _extractLibrary(libFileName,outputFile)

    _libPath = Some(outputFile.getAbsolutePath)
    _isLibExtracted = true

    outputFile
  }

  // FIXME: the library seems to not be copied automatically in the target since we moved to cross Scala versions build
  private def _extractLibrary(libFileName: String, outputFile: File): Unit = {
    assert(!outputFile.exists(), s"library file already exists: $outputFile")

    val in = _classLoader.getResourceAsStream(libFileName)
    val out = Files.newOutputStream(outputFile.toPath)

    try {
      require(in != null, s"Classpath resource '$libFileName' not found")

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

    ()
  }

}
