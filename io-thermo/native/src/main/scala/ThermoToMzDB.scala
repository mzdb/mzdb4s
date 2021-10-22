import java.io.File

import com.github.mzdb4s.db.model.MzDbHeader
import com.github.mzdb4s.db.model.params.{FileContent, ParamTree}
import com.github.mzdb4s.db.model.params.param.UserParam
import com.github.mzdb4s.io.thermo._
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

import scala.scalanative.posix.limits
import scala.scalanative.posix.unistd.readlink
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

object ThermoToMzDB {

  // FIXME: this is a hack, in inject here the pwiz-mzdb version for mzdb-access backward compat, we should update mzdb-access
  val VERSION = "0.9.10" //"0.2" // TODO: manage with SBT

  var errorThrown = 0

  implicit val sf: ISQLiteFactory = SQLiteFactory

  private def readLink(link: CString)(implicit z: Zone): CString = {
    val buffer: CString = alloc[Byte](limits.PATH_MAX)
    readlink(link, buffer, limits.PATH_MAX - 1.toUInt) match {
      case -1 =>
        null
      case read =>
        // readlink doesn't null-terminate the result.
        buffer(read) = 0.toByte
        buffer
    }
  }

  def main2(args: Array[String]): Unit = {
    println("--- Thermo to mzDB file converter ---")

    val assemblyPath: String = Zone { implicit z =>
      val path = readLink(c"/proc/self/exe")
      require(path != null, "can't determine executable location")

      // --- Warning: the trailing slash is required, otherwise Mono is crashing ---
      //new File(fromCString(path)).getParent + "/lib/"
      // TODO: customize DEV versus PROD
      "./io-thermo/shared/src/main/resources/linux_rawfileparser/"
    }
    require(new File(assemblyPath).isDirectory && assemblyPath.endsWith("/"), "can't determine Thermo libraries directory")
    println("Thermo libraries loaded from: " + assemblyPath)

    val rawFileNameOpt = args.headOption.orElse(Some("/mnt/d/Dev/wsl/scala-native/mzdb4s/data/ThermoRawFileParser/UPS1_25000amol_R1.raw"))
    //val rawFileNameOpt = args.headOption.orElse(Some("/mnt/d/Dev/wsl/scala-native/mzdb4s/data/small/small.RAW")) //.orElse(Some("/mnt/d/Dev/wsl/scala-native/mzdb4s/data/ThermoRawFileParser/UPS1_25000amol_R1.raw"))
    require(rawFileNameOpt.isDefined, "An input file name must be provided")
    val rawFileName = rawFileNameOpt.get

    /*val intensityThreshold = if (args.length == 1) 0
    else {
      val threshold = args(1).toInt
      println(s"CONFIG: peaks with an intensity lower than $threshold will be removed from spectra")
      threshold
    }*/

    // --- Set the Mono runtime location --- //
    bindings.MonoEmbeddinator.mono_embeddinator_set_runtime_assembly_path(c"/usr/lib/")

    // --- Set the C# shared library location (should be the 'lib' sub-driectory of the current application) --- //
    //bindings.MonoEmbeddinator.mono_embeddinator_set_assembly_path(c"/mnt/d/Dev/wsl/scala-native/mzdb4s/io-thermo/shared/src/main/resources/mzdb-conversion-tools_0.2/lib/")
    Zone { implicit z =>
      bindings.MonoEmbeddinator.mono_embeddinator_set_assembly_path(toCString(assemblyPath))
    }

    val errCb = scala.scalanative.unsafe.CFuncPtr1.fromScalaFunction((error: bindings.MonoEmbeddinator.mono_embeddinator_error_t) => {
      //ThermoToMzDB.errorThrown += 1
      ()
    })
    bindings.MonoEmbeddinator.mono_embeddinator_install_error_report_hook(errCb)

    // Show the Thermo RawFileParser version
    val libVersion = fromCString(bindings.ThermoRawFileParser.ThermoRawFileParser_MainClass_get_Version())
    println(s"ThermoRawFileParser version '$libVersion' has been successfully loaded")

    try {
      val writtenMzDbFile = new File( rawFileName +".mzDB" )
      writtenMzDbFile.delete() // TODO: remove me
      if (writtenMzDbFile.exists()) {
        System.err.println(s"mzDB file already exists: $writtenMzDbFile")
        System.exit(1)
      }

      this.convert(rawFileName, writtenMzDbFile)
    } finally {
      if (errorThrown > 0)
        println(s"$errorThrown error(s) were thrown from C# but not caught")
    }

    System.exit(0)
  }

  def convert(rawFilePath: String, mzDbFile: File): Unit = {

    var startTime = System.currentTimeMillis

    // FIXME: determine the ByteOrder
    val bo = java.nio.ByteOrder.LITTLE_ENDIAN

    // FIXME: create alternative DEs for NO LOSS mode (64-bit intensities)
    val profileHighResDE = DataEncoding(id = 1, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val profileLowResDE = DataEncoding(id = 2, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedHighResDE = DataEncoding(id = 3, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedLowResDE = DataEncoding(id = 4, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)

    implicit val sf: ISQLiteFactory = SQLiteFactory

    println("Opening raw file located at: " + rawFilePath)

    val rawFileStreamer = RawFileParserWrapper.getRawFileStreamer(rawFilePath)

    val mzMLMetaData = rawFileStreamer.getMetaData(VERSION)

    val mzDbMetaData = mzMLMetaData.toMzDbMetaData(
      mzDbHeader = new MzDbHeader(
        version = "0.7",
        creationTimestamp = new java.util.Date().getTime.toInt / 1000,
        mzMLMetaData.fileContent,
        paramTree = ParamTree(
          userParams = List(
            UserParam(name = "origin_file_format", value = "Thermo RAW format", `type` = "xsd:string")
          )
        )
      ),
      dataEncodings = Seq.empty[DataEncoding] // should be determined dynamically
    )

    var took = (System.currentTimeMillis - startTime).toFloat / 1000
    println("Raw file initialization took: " + took)

    println("Writing mzDB file located at: " + mzDbFile.getAbsolutePath)

    val mzDbWriter = new MzDbWriter(
      mzDbFile,
      mzDbMetaData,
      DefaultBBSizes(),
      isDIA = false
    )
    mzDbWriter.open()

    println("Reading spectra from raw file...")

    startTime = System.currentTimeMillis

    var i = 0
    rawFileStreamer.forEachSpectrum { spectrum =>
      i += 1

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList )

      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

      mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, centroidedHighResDE)

      if (i % 100 == 0) {
        println(s"Processed $i spectra...")
      }

      //if (i == 5000) false else true

      true
    }

    println(s"Processed a total of $i spectra.")

    took = (System.currentTimeMillis - startTime).toFloat / 1000
    println("Processing spectra took: " + took)

    /*startTime = System.currentTimeMillis

    val backup = mzDbWriter.getConnection().asInstanceOf[com.github.sqlite4s.SQLiteConnectionWrapper].conn.initializeBackup(mzDbFile)
    try {
      while (!backup.isFinished) {
        backup.backupStep(32)
      }
    } finally {
      backup.dispose()
    }

    took = (System.currentTimeMillis - startTime).toFloat / 1000
    println("Writing spectra to disk took: " + took)*/

    println("Closing mzDB writer...")
    mzDbWriter.close()
  }
}
