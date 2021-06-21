import java.io.File
import mainargs._
import quickxml.QuickXmlEnv
import com.github.mzdb4s.io.thermo.RawFileParserEnv

object MzDbTools extends AbstractMzDbTools  {

  // Configure english locale
  java.util.Locale.setDefault(java.util.Locale.ENGLISH)

  // Configure SQLite4Java
  com.almworks.sqlite4java.SQLite.setLibraryPath(NATIVE_LIB_DIR_PATH)

  // Configure JFFI
  //System.setProperty("java.library.path",NATIVE_LIB_DIR_PATH) // TODO: is it used?
  //System.setProperty("jffi.boot.library.path",NATIVE_LIB_DIR_PATH)
  System.setProperty("jffi.extract.dir",NATIVE_LIB_DIR_PATH)
  System.setProperty("jffi.extract.name","") // requires to extract the library without mangling its name

  // Retrieve the list of existing/cached native files in NATIVE_LIB_DIR
  private val nativeFiles = NATIVE_LIB_DIR.listFiles()

  // Configure QuickXml native library
  private lazy val quickxmlLibFileOpt = nativeFiles.find(_.getName == QuickXmlEnv.libraryFileName)
  if (quickxmlLibFileOpt.isDefined) {
    QuickXmlEnv.setLibraryPath(quickxmlLibFileOpt.get.getAbsolutePath)
  } else {
    QuickXmlEnv.extractLibrary(Some(NATIVE_LIB_DIR))
  }

  // Configure native libraries of the raw file parser wrapper
  private val rawFileParserLibFileOpt = nativeFiles.find(_.getName == RawFileParserEnv.libraryFileName)

  private def _checkRawFileParseLib(): Unit = {
    if (rawFileParserLibFileOpt.isDefined) {
      RawFileParserEnv.setLibraryFile(rawFileParserLibFileOpt.get)
    } else {
      RawFileParserEnv.extractLibrary(Some(NATIVE_LIB_DIR))
    }
  }

  protected def getAssemblyDir(): File = {
    // Return the current JAR directory
    new File(this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParentFile
  }

  @main
  def mzdb2mgf(
    @arg(short = 'i', doc = "Path to the mzDB input file")
    mzdb: String,
    @arg(short = 'o', name = "mgf", doc = "Path to the MGF output file")
    mgfOpt: Option[String]
  ): Unit = {
    this._mzdb2mgf(mzdb,mgfOpt.getOrElse(mzdb + ".mgf"))
  }

  @main
  def thermo2mzdb(
    @arg(short = 'i', doc = "Path to the raw input file")
    raw: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String]
  ): Unit = {

    _checkRawFileParseLib()

    // Apply temporary workaround for Linux OS, because Mono is currently crashing inside the JVM (segfault)
    val isLinux = System.getProperty("os.name").toLowerCase().startsWith("linux")
    if (isLinux) {

      // Retrieve current JAR directory
      val jarDir = getAssemblyDir()

      // Execute "chmod" and sends output to stdout
      import scala.sys.process._
      s"chmod +x $jarDir/mzdbtools".!

      // Retrieve and update LD_LIBRARY_PATH
      var ldLibPath = scala.util.Properties.envOrElse("LD_LIBRARY_PATH", "")
      ldLibPath = if (ldLibPath.isEmpty) NATIVE_LIB_DIR_PATH else s"$ldLibPath:$NATIVE_LIB_DIR_PATH"

      // Execute mzdbtools
      val command = if (mzdbOpt.isEmpty) List(s"$jarDir/mzdbtools","thermo2mzdb","-i",raw)
      else List(s"$jarDir/mzdbtools","thermo2mzdb","-i",raw,"-o",mzdbOpt.get)

      Process(command.mkString(" "),None,("LD_LIBRARY_PATH",ldLibPath)).!
    } else {
      this._thermo2mzdb(raw,mzdbOpt.getOrElse(raw + ".mzDB"))
    }
  }

  @main
  def tdf2mzdb(
    @arg(short = 'i', name = "tdf-dir", doc = "Path to the TDF input directory")
    tdfDir: String,
    @arg(short = 'o', doc = "Path to the mzDB output file")
    mzdbOpt: Option[String]
  ): Unit = {

    import com.github.mzdb4s.io.timsdata._

    // Apply temporary workaround for Linux OS, because JFFI is crashing (long_2nd is not assignable to integer in TimsReaderLibrary$OnEachFrameCallback)
    if(System.getProperty("os.name").toLowerCase().startsWith("linux")) {
      //TimsReaderLibrary.loadLibrary(new File(s"$NATIVE_LIB_DIR_PATH/libtimsdatareader.so").getAbsolutePath)

      // Retrieve current JAR directory
      val jarDir = getAssemblyDir()

      // Execute "chmod" and sends output to stdout
      import scala.sys.process._
      s"chmod +x $jarDir/mzdbtools".!

      // Retrieve and update LD_LIBRARY_PATH
      var ldLibPath = scala.util.Properties.envOrElse("LD_LIBRARY_PATH", "")
      ldLibPath = if (ldLibPath.isEmpty) NATIVE_LIB_DIR_PATH else s"$ldLibPath:$NATIVE_LIB_DIR_PATH"

      // Execute mzdbtools
      val command = if (mzdbOpt.isEmpty) List(s"$jarDir/mzdbtools","tdf2mzdb","-i",tdfDir)
      else List(s"$jarDir/mzdbtools","tdf2mzdb","-i",tdfDir,"-o",mzdbOpt.get)

      Process(command.mkString(" "),None,("LD_LIBRARY_PATH",ldLibPath)).!

    } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
      TimsReaderLibraryFactory.loadLibrary(new File(s"$NATIVE_LIB_DIR_PATH/timsdatareader.dll").getAbsolutePath)

      this._tdf2mzdb(tdfDir,mzdbOpt.getOrElse(tdfDir + ".mzDB"))
    } else {
      throw new Exception("Unsupported Operating System")
    }

    //System.err.println("Not yet implemented!")
    //System.exit(-1)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}