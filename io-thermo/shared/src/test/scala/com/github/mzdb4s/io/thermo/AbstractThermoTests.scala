package com.github.mzdb4s.io.thermo

import com.github.mzdb4s.db.model.MzDbHeader
import com.github.mzdb4s.db.model.params.ParamTree
import com.github.mzdb4s.db.model.params.param.UserParam
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata.{DataEncoding, DataMode, DefaultBBSizes, PeakEncoding, SpectrumData}
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

import utest._

import java.io.File

trait AbstractThermoTests extends TestSuite {

  // FIXME: this is a hack, in inject here the pwiz-mzdb version for mzdb-access backward compat, we should update mzdb-access
  protected val SOFTWARE_VERSION = "0.9.10" //"0.2" // TODO: manage with SBT

  def initRawFileParserWrapper(): IRawFileParserWrapper

  private val wrapper = initRawFileParserWrapper()

  protected def getProjectDir(): String = {
    val parentDir = new java.io.File("./").getAbsoluteFile.getParentFile.getName
    if (parentDir == "jvm" || parentDir == "native") {
      ".."
    } else {
      "./io-thermo"
    }
  }

  protected def getResourcesDir(): File = {
    val resourcesDir = getProjectDir() + "/shared/src/test/resources"
    new File(resourcesDir)
  }

  private lazy val smallRawFile: File = {
    new File(getResourcesDir(),"small.RAW")
  }

  private lazy val mzDbFile: File = File.createTempFile("small-",".mzDB")

  override def utestAfterAll(): Unit = {
    mzDbFile.delete()
  }

  val tests: Tests = Tests {
    test("readSmallRawFile") - readSmallRawFile()
    test("convertSmallRawFile") - convertSmallRawFile()
  }

  def readSmallRawFile(): Unit = {

    val rawFile = smallRawFile

    val startTime = System.currentTimeMillis

    val streamer = wrapper.getRawFileStreamer(rawFile.getPath)

    println("Starting spectra iteration after " + (System.currentTimeMillis - startTime) + "ms")

    var i = 0
    streamer.forEachSpectrum { spectrum =>
      i += 1
      true
    }

    println("Raw file streamer disposed!")

    val took = System.currentTimeMillis - startTime
    println(s"Read $i spectra in $took ms")
  }

  def convertSmallRawFile(): Unit = {

    val rawFile = smallRawFile
    val rawFilePath = rawFile.getPath
    //val mzDbFile = File.createTempFile("small-",".mzDB")
    //mzDbFile.deleteOnExit()

    // FIXME: determine the ByteOrder
    val bo = java.nio.ByteOrder.LITTLE_ENDIAN

    // FIXME: create alternative DEs for NO LOSS mode (64-bit intensities)
    val profileHighResDE = DataEncoding(id = 1, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val profileLowResDE = DataEncoding(id = 2, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedHighResDE = DataEncoding(id = 3, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedLowResDE = DataEncoding(id = 4, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)

    implicit val sf: ISQLiteFactory = SQLiteFactory
    //sf.configureLogging(com.github.sqlite4s.LogLevel.OFF)

    println("Opening raw file located at: " + rawFilePath)

    val rawFileStreamer = wrapper.getRawFileStreamer(rawFilePath)

    val mzMLMetaData = rawFileStreamer.getMetaData(SOFTWARE_VERSION)

    val mzDbMetaData = mzMLMetaData.toMzDbMetaData(
      mzDbHeader = new MzDbHeader(
        version = "0.7",
        creationTimestamp = new java.util.Date().getTime.toInt / 1000,
        mzMLMetaData.fileContent,
        paramTree = ParamTree()
      ),
      dataEncodings = Seq.empty[DataEncoding] // should be determined dynamically
    )

    println("Writing mzDB file located at: " + mzDbFile.getAbsolutePath)

    val mzDbWriter = new MzDbWriter(
      mzDbFile,
      mzDbMetaData,
      DefaultBBSizes(),
      isDIA = false
    )
    mzDbWriter.open()

    println("Reading spectra from raw file...")

    rawFileStreamer.forEachSpectrum { spectrum =>

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList.map(_.toFloat) )

      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

      mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, centroidedHighResDE)

      if (spectrum.header.id % 100 == 0) {
        println(s"Processed ${spectrum.header.id} spectra...")
      }

      true
    }

    mzDbWriter.close()

    println("Closing mzDB writer...")
  }

}
