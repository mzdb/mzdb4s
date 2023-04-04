import com.github.mzdb4s.MzDbReader

import java.io.File
import scala.collection.mutable.ListBuffer
import mainargs._
import quickxml.QuickXmlEnv
import com.github.mzdb4s.io.thermo.RawFileParserEnv
import com.github.mzdb4s.io.writer.MzDbWriter

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
  private lazy val nativeFiles = NATIVE_LIB_DIR.listFiles()

  // Configure QuickXml native library
  private def _configureQuickXmlNativeLib(): Option[File] = {
    val libFileOpt = nativeFiles.find(_.getName == QuickXmlEnv.libraryFileName)

    if (libFileOpt.isDefined) {
      QuickXmlEnv.setLibraryPath(libFileOpt.get.getAbsolutePath)
    } else {
      QuickXmlEnv.extractLibrary(Some(NATIVE_LIB_DIR))
    }

    libFileOpt
  }

  private def _cconfigureNativeLibsForThermoConversion(): Unit = {
    _configureQuickXmlNativeLib()

    // Configure native libraries of the raw file parser wrapper
    val rawFileParserLibFileOpt = nativeFiles.find(_.getName == RawFileParserEnv.libraryFileName)
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
    @arg(short = 'i', name = "mzdb", doc = "Path to the mzDB input file")
    mzdb: String,
    @arg(short = 'o', name = "mgf", doc = "Path to the MGF output file")
    mgfOpt: Option[String]
  ): Unit = {
    this._mzdb2mgf(mzdb,mgfOpt.getOrElse(mzdb + ".mgf"))
  }

  @main
  def thermo2mzdb(
    @arg(short = 'i', name = "raw", doc = "Path to the raw input file")
    raw: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String],
    @arg(short = 's', name = "split-faims", doc = "Split RAW file in multiple mzDB files (for each FAIMS CV value)")
    splitFaims: Flag
  ): Unit = {

    _cconfigureNativeLibsForThermoConversion()

    // Apply temporary workaround for Linux OS, because Mono is currently crashing inside the JVM (segfault)
    val isLinux = System.getProperty("os.name").toLowerCase().startsWith("linux")

    // Retrieve current JAR directory
    val jarDir = getAssemblyDir()

    if (isLinux && new File(s"$jarDir/mzdbtools").isFile) {

      // Execute "chmod" and sends output to stdout
      import scala.sys.process._
      s"chmod +x $jarDir/mzdbtools".!

      // Retrieve and update LD_LIBRARY_PATH
      var ldLibPath = scala.util.Properties.envOrElse("LD_LIBRARY_PATH", "")
      ldLibPath = if (ldLibPath.isEmpty) NATIVE_LIB_DIR_PATH else s"$ldLibPath:$NATIVE_LIB_DIR_PATH"

      // Create the list of command arguments
      val commandArgs = ListBuffer(s"$jarDir/mzdbtools","thermo2mzdb","-i",raw,"-o",mzdbOpt.getOrElse(raw + ".mzDB"))
      if (splitFaims.value) commandArgs += "--split-faims"

      // Execute mzdbtools
      Process(commandArgs.mkString(" "),None,("LD_LIBRARY_PATH",ldLibPath)).!
    } else {
      this._thermo2mzdb(raw,mzdbOpt.getOrElse(raw + ".mzDB"), splitFaims.value)
    }
  }

  @main
  def tdf2mzdb(
    @arg(short = 'i', name = "tdf-dir", doc = "Path to the TDF input directory")
    tdfDir: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String],
    @arg(name = "mzdist", doc = "Minimum distance between consecutive m/z values (PPM unit)")
    mzdistOpt: Option[Int]
  ): Unit = {

    val mzTolPPM = mzdistOpt.getOrElse(10)

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
      val command = if (mzdbOpt.isEmpty) List(s"$jarDir/mzdbtools","tdf2mzdb","-i",tdfDir,"--mzdist",mzTolPPM)
      else List(s"$jarDir/mzdbtools","tdf2mzdb","-i",tdfDir,"-o",mzdbOpt.get,"--mzdist",mzTolPPM)

      Process(command.mkString(" "),None,("LD_LIBRARY_PATH",ldLibPath)).!

    } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
      TimsReaderLibraryFactory.loadLibrary(new File(s"$NATIVE_LIB_DIR_PATH/timsdatareader.dll").getAbsolutePath)

      // FIXME: parse config from command line
      val config: Option[TimsDataReaderConfig] = Some(
        TimsDataReaderConfig(
          ms1Only = true,
          mzTolPPM = mzTolPPM,
          minDataPointsCount = 1,
          nThreads = 4,
          nTimsdataDllThreads = 4,
        )
      )

      this._tdf2mzdb(tdfDir,mzdbOpt.getOrElse(tdfDir + ".mzDB"), config)
    } else {
      throw new Exception("Unsupported Operating System")
    }

  }

  /*
  @main
  def remap_ms2(
    @arg(short = 'i', name = "input", doc = "Path to the mzDB input file")
    inputMzdb: String,
    @arg(short = 'o', name = "output", doc = "Path to the MGF output file")
    outputMzdb: String,
  ): Unit = {

    logger.info("--- mzDB to MGF file converter ---")

    val mzDbFile = new File(inputMzdb)
    require(mzDbFile.isFile, s"can't find an mzDB file at: ${mzDbFile.getAbsolutePath}")

    logger.info(s"Loading data from mzDB located at: ${mzDbFile.getAbsolutePath}")

    // Create mzDB reader/writer
    val mzDbReader = new MzDbReader(mzDbFile, false)

    import collection.mutable.ArrayBuffer
    import com.github.mzdb4s.util.collection._
    import com.github.mzdb4s.msdata._
    try {
      val ms1SpecByCycle = new scala.collection.mutable.LongMap[Spectrum]()

      //val mzDbReaderVero = new MzDbReader("E:\\LCMS\\data_timstof_strasbourg\\dataset_magali\\TP7718MS_Slot1-58_1_7773.d_20220208-08_39_33.mzdb", false)
      val ms1SpecIter = mzDbReader.getSpectrumIterator(1)
      while (ms1SpecIter.hasNext()) {
        val ms1Spectrum = ms1SpecIter.next()
        ms1SpecByCycle.put(ms1Spectrum.header.cycle, ms1Spectrum)
      }
      //mzDbReaderVero.close()

      def _findNearestParent(precMz: Double, mzTolPPM: Int, curCycleNum: Int, cycleNumRef: Int, cycleStep: Int, maxCycleDist: Int): Int = {
        val parentSpectrumOpt = ms1SpecByCycle.get(curCycleNum)
        if (parentSpectrumOpt.isEmpty) return -1

        val parentSpectrum = parentSpectrumOpt.get
        val nearestPeak = parentSpectrumOpt.get.getData.getNearestPeak(precMz, mzTolPPM, parentSpectrum.header)
        if (nearestPeak != null) {
          curCycleNum
        } else if (Math.abs(curCycleNum - cycleNumRef) <= maxCycleDist) {
          _findNearestParent(precMz,mzTolPPM, curCycleNum + cycleStep, cycleNumRef, cycleStep, maxCycleDist)
        } else {
          -1
        }
      }

      val ms2SpecsByCycle = new scala.collection.mutable.LongMap[ArrayBuffer[Spectrum]]()

      var totalMs2SpectraCount = 0
      var nonMappedMs2Count = 0
      val ms2SpecIter = mzDbReader.getSpectrumIterator(2)
      while (ms2SpecIter.hasNext()) {
        totalMs2SpectraCount += 1
        val spectrum = ms2SpecIter.next()
        val sh = spectrum.header
        val precMzOpt = sh.getPrecursorMz

        val nearestCycleOpt = if (precMzOpt.isEmpty) None
        else {
          val curCycle = sh.cycle
          val nearestForwardCycle = _findNearestParent(precMzOpt.get, 15, curCycle, curCycle, 1, 0)
          val nearestBackardCycle = _findNearestParent(precMzOpt.get, 15, curCycle, curCycle, -1, 0)

          if (nearestForwardCycle == -1 && nearestBackardCycle == -1) {
            None
          } else if (nearestBackardCycle == -1) {
            Some(nearestForwardCycle)
          } else if (nearestForwardCycle == -1) {
            Some(nearestBackardCycle)
          } else if ( Math.abs(nearestForwardCycle - curCycle) <= Math.abs(nearestBackardCycle - curCycle) ) {
            Some(nearestForwardCycle)
          } else {
            Some(nearestBackardCycle)
          }
        }

        val msCycle = if (nearestCycleOpt.isDefined) {
          //println("Nearest cycle is: " + nearestCycleOpt.get)
          nearestCycleOpt.get
        } else {
          nonMappedMs2Count += 1
          //println("No nearest cycle found for: " + precMzOpt)
          sh.cycle
        }

        assert(msCycle != -1, "MS cycle number must be greater than zero")
        assert(ms1SpecByCycle.contains(msCycle), "invalid MS cycle number")

        // Re-map MS2 spectrum to a new MS cycle
        ms2SpecsByCycle.getOrElseUpdate(msCycle, new ArrayBuffer[Spectrum]) += spectrum
      }

      println(s"Has iterated over $totalMs2SpectraCount MS/MS spectra")

      if (nonMappedMs2Count > 0) {
        logger.warn(s"$nonMappedMs2Count MS/MS spectra can't be mapped correctly (no signal found in parent MS survey)")
      }

      // --- Write the remapped spectra --- //
      val dataEncBySpecId = mzDbReader.getDataEncodingBySpectrumId()
      val spectrumXmlMetaDataById = mzDbReader.getSpectraXmlMetaData().mapByLong(_.spectrumId)

      val mzDbWriter = new MzDbWriter(
        new File(outputMzdb),
        mzDbReader.getMetaData(),
        DefaultBBSizes(), //  FIXME: get from previous mzDB or from command line?
        isDIA = false
      )
      mzDbWriter.open()

      val msCycles = ms1SpecByCycle.keys.toArray
      msCycles.sortInPlace()

      for (msCycle <- msCycles) {
        val ms1Spec = ms1SpecByCycle(msCycle)
        val sh = ms1Spec.header
        mzDbWriter.insertSpectrum(
          ms1Spec,
          spectrumXmlMetaDataById(sh.id),
          dataEncBySpecId(sh.id)
        )

        val ms2SpecsOpt = ms2SpecsByCycle.get(sh.cycle)

        if (ms2SpecsOpt.isDefined) {
          val ms2Specs = ms2SpecsOpt.get.sortBy(_.header.id)

          var tmpRtOffset = 1
          for (ms2Spec <- ms2Specs) {
            val ms2SpecId = ms2Spec.header.id
            val newMs2Spec = ms2Spec.copy(header = ms2Spec.header.copy(
              cycle = sh.cycle,
              time = sh.time + (tmpRtOffset.toFloat) / 1000
            ))

            mzDbWriter.insertSpectrum(
              newMs2Spec,
              spectrumXmlMetaDataById(ms2SpecId),
              dataEncBySpecId(ms2SpecId)
            )

            tmpRtOffset += 1
          }
        }
      }

      mzDbWriter.close()

    } finally {
      mzDbReader.close()
    }

    logger.info("remapping of mzDB MS/MS data has completed :)")
  }*/

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}