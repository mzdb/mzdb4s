import java.io.File
import mainargs._

import com.github.mzdb4s._
import com.github.mzdb4s.db.model.MzDbHeader
import com.github.mzdb4s.db.model.params.ParamTree
import com.github.mzdb4s.io.thermo.RawFileParserWrapper
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

abstract class AbstractMzDbTools extends Logging {

  protected def getAssemblyDir(): File

  // Define the directory containing native libraries for JFFI and SQLite4Java
  protected val NATIVE_LIB_DIR = new File(getAssemblyDir(), "lib").getAbsoluteFile.getCanonicalFile
  protected val NATIVE_LIB_DIR_PATH = NATIVE_LIB_DIR.getAbsolutePath

  // Configure mzdb4s logging
  Logging.configureLogger(LogLevel.DEBUG)

  // Intialize SQLite and disable its internal logging
  implicit val sf: ISQLiteFactory = SQLiteFactory
  sf.configureLogging(com.github.sqlite4s.LogLevel.OFF)

  protected def _mzdb2mgf(
    mzDbPath: String,
    mgfPath: String
  ): Unit = {
    logger.info("--- mzDB to MGF file converter ---")

    val mzDbFile = new File(mzDbPath)
    require(new File(mzDbPath).isFile, s"can't find an mzDB file at: ${mzDbFile.getAbsolutePath}")

    import com.github.mzdb4s.io.writer.MgfWriter
    logger.info("Creating MGF file for mzDB located at: " + mzDbFile.getAbsolutePath)
    //println("Precursor m/z values will be defined using the method: " + cmd.precMzComputation)

    val writer = MgfWriter(mzDbPath,mgfFile = Some(mgfPath))
    writer.write()

    logger.info("mzDB -> MGF conversion has completed :)")
  }

  protected  def _thermo2mzdb(
    rawPath: String,
    mzDbPath: String
  ): Unit = {
    logger.info("--- raw to mzDB file converter ---")

    val rawFile = new File(rawPath)
    require(rawFile.isFile, s"can't find a raw file at: ${rawFile.getAbsolutePath}")

    val mzDbFile = new File(mzDbPath)
    require(!mzDbFile.exists(), s"can't create mzDB file because it already exists at: ${mzDbFile.getAbsolutePath}")

    val wrapper = RawFileParserWrapper
    wrapper.initialize(new File(NATIVE_LIB_DIR, "rawfileparser").getCanonicalPath)
    //wrapper.initialize(NATIVE_LIB_DIR_PATH)

    // FIXME: determine the ByteOrder
    val bo = java.nio.ByteOrder.LITTLE_ENDIAN

    //val profileHighResDE = DataEncoding(id = 1, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    //val profileLowResDE = DataEncoding(id = 2, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedHighResDE = DataEncoding(id = 3, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    //val centroidedLowResDE = DataEncoding(id = 4, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)

    implicit val sf: ISQLiteFactory = SQLiteFactory
    //sf.configureLogging(com.github.sqlite4s.LogLevel.OFF)

    logger.info("Opening raw file located at: " + rawFile.getAbsolutePath)

    val rawFileStreamer = wrapper.getRawFileStreamer(rawPath)

    // FIXME: this is a hack, we inject here the pwiz-mzdb version for mzdb-access backward compat, we should update mzdb-access
    val mzMLMetaData = rawFileStreamer.getMetaData("0.9.10") // TODO: manage with SBT

    val mzDbMetaData = mzMLMetaData.toMzDbMetaData(
      mzDbHeader = new MzDbHeader(
        version = "0.7",
        creationTimestamp = new java.util.Date().getTime.toInt / 1000,
        mzMLMetaData.fileContent,
        paramTree = ParamTree()
      ),
      dataEncodings = Seq.empty[DataEncoding] // should be determined dynamically
    )

    logger.info("Writing mzDB file located at: " + mzDbFile.getAbsolutePath)

    val mzDbWriter = new MzDbWriter(
      mzDbFile,
      mzDbMetaData,
      DefaultBBSizes(),
      isDIA = false // FIXME: we should infer this
    )
    mzDbWriter.open()

    logger.info("Reading spectra from raw file...")

    rawFileStreamer.forEachSpectrum { spectrum =>

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList)

      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

      mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, centroidedHighResDE)

      if (spectrum.header.id % 1000 == 0) {
        logger.debug(s"Processed ${spectrum.header.id} spectra...")
      }

      true
    }

    logger.debug("Closing mzDB writer...")
    mzDbWriter.close()

    logger.info("raw -> mzDB conversion has completed :)")
  }

  protected  def _tdf2mzdb(
    tdfDir: String,
    mzDbPath: String
  ): Unit = {

    import com.github.mzdb4s.io.timsdata._
    import com.github.sqlite4s.SQLiteFactory

    logger.info("--- TDF to mzDB file converter ---")

    require(new File(tdfDir).isDirectory, s"can't find a directory at: $tdfDir")
    require(new File(tdfDir,"analysis.tdf").isFile, s"can't find an 'analysis.tdf' file in the directory: $tdfDir")

    val mzDbFile = new File(mzDbPath)
    require(!mzDbFile.exists(), s"can't create mzDB file because it already exists at: ${mzDbFile.getAbsolutePath}")

    logger.info("Writing mzDB file located at: " + mzDbFile.getAbsolutePath)

    implicit val sf = SQLiteFactory
    implicit val reader = TimsDataReader

    TimsReaderLibraryFactory.initLogger()

    val converter = new TimsData2MzDb(tdfDir, mzDbPath)
    converter.convert()

    logger.info("TDF -> mzDB conversion has completed :)")
  }

}
