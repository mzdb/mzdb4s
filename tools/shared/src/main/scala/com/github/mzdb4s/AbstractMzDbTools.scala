package com.github.mzdb4s

import java.io.File
import com.github.mzdb4s.db.model.{MzDbHeader, MzDbMetaData, SpectrumXmlMetaData}
import com.github.mzdb4s.db.model.params.ParamTree
import com.github.mzdb4s.db.model.params.param.PsiMsCV
import com.github.mzdb4s.io.thermo.{RawFileParserWrapper, RawFileStreamer}
import com.github.mzdb4s.io.timsdata.TimsDataReaderConfig
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

import scala.collection.mutable.ArrayBuffer

abstract class AbstractMzDbTools extends Logging {

  protected def getAssemblyDir(): File

  // Define the directory containing native libraries for JFFI and SQLite4Java
  protected lazy val NATIVE_LIB_DIR = new File(getAssemblyDir(), "lib").getAbsoluteFile.getCanonicalFile
  protected lazy val NATIVE_LIB_DIR_PATH = NATIVE_LIB_DIR.getAbsolutePath

  // Initialize SQLite and disable its internal logging (must be done first to properly apply the mzdb4s config)
  implicit val sf: ISQLiteFactory = SQLiteFactory
  sf.configureLogging(com.github.mzdb4s.LogLevel.OFF)

  // Configure mzdb4s logging
  Logging.configureLogger(LogLevel.DEBUG)

  protected def _mzdb2mgf(
    mzDbPath: String,
    mgfPath: String
  ): Unit = {
    logger.info("--- mzDB to MGF file converter ---")

    try {
      val mzDbFile = new File(mzDbPath)
      require(mzDbFile.isFile, s"can't find an mzDB file at: ${mzDbFile.getAbsolutePath}")

      import com.github.mzdb4s.io.writer.MgfWriter
      logger.info(s"Creating MGF file for mzDB located at: ${mzDbFile.getAbsolutePath}")
      //println("Precursor m/z values will be defined using the method: " + cmd.precMzComputation)

      val writer = MgfWriter(mzDbPath,mgfFile = Some(mgfPath))
      writer.write()

      logger.info("mzDB -> MGF conversion has completed :)")

      System.exit(0)
    } catch {
      case t: Throwable => {
        logger.error("mzDB -> MGF conversion has failed :(", t)
        System.exit(1)
      }
    }

  }

  protected def _thermo2mzdb(
    rawPath: String,
    mzDbPath: String,
    splitFaims: Boolean,
    mergeWiSimSpectra: Boolean
  ): Unit = {
    logger.info("--- raw to mzDB file converter ---")

    try {

      val rawFile = new File(rawPath)
      require(rawFile.isFile, s"can't find a raw file at: ${rawFile.getAbsolutePath}")

      val mzDbFile = new File(mzDbPath)
      // Note: the requirement is now postponed after the FAIMS check (see below)
      //require(!mzDbFile.exists(), s"can't create mzDB file because it already exists at: ${mzDbFile.getAbsolutePath}")

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

      logger.info(s"Opening raw file located at: ${rawFile.getAbsolutePath}")

      // Open the raw file to perform the conversion
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

      logger.info("Reading spectra from raw file...")

      if (splitFaims) {
        this._thermo2mzdbFAIMS(mzDbFile,rawFileStreamer, mzDbMetaData, centroidedHighResDE)
      } else if (mergeWiSimSpectra) {
        this._thermo2mzdbWiSIM(mzDbFile,rawFileStreamer, mzDbMetaData, centroidedHighResDE)
      } else {
        this._thermo2mzdbNormal(mzDbFile,rawFileStreamer, mzDbMetaData, centroidedHighResDE)
      }

      logger.info("raw -> mzDB conversion has completed :)")

      System.exit(0)
    } catch {
      case t: Throwable => {
        logger.error("raw -> mzDB conversion has failed :(", t)
        System.exit(1)
      }
    }
  }

  private def _createMzDbWriter(outputFile: File, mzDbMetaData: MzDbMetaData): MzDbWriter = {
    require(!outputFile.exists(), s"can't create mzDB file because it already exists at: ${outputFile.getAbsolutePath}")

    logger.info(s"Writing mzDB file located at: ${outputFile.getAbsolutePath}")

    val mzDbWriter = new MzDbWriter(
      outputFile,
      mzDbMetaData,
      DefaultBBSizes(),
      isDIA = false // FIXME: we should infer this
    )
    mzDbWriter.open()

    mzDbWriter
  }

  private def _thermo2mzdbNormal(mzDbFile: File, rawFileStreamer: RawFileStreamer, mzDbMetaData: MzDbMetaData, dataEncoding: DataEncoding): Unit = {
    val mzDbWriter = _createMzDbWriter(mzDbFile, mzDbMetaData)

    rawFileStreamer.forEachSpectrum { spectrum =>

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList)
      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

      mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, dataEncoding)

      if (spectrum.header.id % 1000 == 0) {
        logger.debug(s"Processed ${spectrum.header.id} spectra...")
      }

      true
    }

    logger.debug("Closing mzDB writer...")
    mzDbWriter.close()
  }

  private def _thermo2mzdbFAIMS(mzDbFile: File, rawFileStreamer: RawFileStreamer, mzDbMetaData: MzDbMetaData, dataEncoding: DataEncoding): Unit = {
    val mzDbWriterByFaimsCv = new collection.mutable.HashMap[Double, MzDbWriter]

    rawFileStreamer.forEachSpectrum { spectrum =>

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList)
      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

        val filterStringCvParam = spectrum.header.getScanList.getScans().head.getCVParam(PsiMsCV.FILTER_STRING)

        var inserted = false // Ensure we don't insert the same spectrum twice
        for (
          subStr <- filterStringCvParam.value.split(' ')
          if subStr.length > 3 && subStr.charAt(0) == 'c' && subStr.charAt(1) == 'v' && ! inserted
        ) {
          val faimsCv = subStr.split('=')(1).toFloat
          val mzDbFileNameParts = mzDbFile.getName.split('.')
          mzDbFileNameParts(0) = mzDbFileNameParts(0) + "_CV" + (-faimsCv).toInt
          val faimsMzDbFile = new File(mzDbFile.getParentFile, mzDbFileNameParts.mkString("."))

          val mzDbWriter = mzDbWriterByFaimsCv.getOrElseUpdate(faimsCv, _createMzDbWriter(faimsMzDbFile, mzDbMetaData))
          mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, dataEncoding)

          inserted = true
      }

      if (spectrum.header.id % 1000 == 0) {
        logger.debug(s"Processed ${spectrum.header.id} spectra...")
      }

      true
    }

    logger.debug("Closing FAIMS mzDB writers...")
    mzDbWriterByFaimsCv.values.foreach(_.close())
  }

  private def _thermo2mzdbWiSIM(mzDbFile: File, rawFileStreamer: RawFileStreamer, mzDbMetaData: MzDbMetaData, dataEncoding: DataEncoding): Unit = {
    val mzDbWriter = _createMzDbWriter(mzDbFile, mzDbMetaData)

    val mzCalibratorBuffer = new RingBuffer[Double](6)
    var prevCalibratedSpectrum: Spectrum = null
    val spectraBuffer = new ArrayBuffer[(Spectrum, SpectrumXmlMetaData)]
    rawFileStreamer.forEachSpectrum { spectrum =>

      val mzDbSpectrumData = new SpectrumData(spectrum.mzList, spectrum.intensityList)
      val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(spectrum.header, mzDbSpectrumData)

      if (mzDbSpectrum.header.msLevel == 1) {
        val filterStringCvParam = spectrum.header.getScanList.getScans().head.getCVParam(PsiMsCV.FILTER_STRING)
        // Discard "Full ms" spectra and keep only "SIM ms" ones
        // FIXME: "SIM" events are not always flagged as expected, thus we also consider centroided Full MS events
        if (filterStringCvParam.value.contains("SIM") || filterStringCvParam.value.contains("FTMS + c NSI Full ms")) {
          spectraBuffer += (mzDbSpectrum -> spectrum.xmlMetaData)
        }

      } else {
        if (spectraBuffer.nonEmpty) {
          val (mergedSpectrum, specXmlMetaData) = _mergeWiSIMSpectra(mzDbWriter, spectraBuffer, dataEncoding)
          spectraBuffer.clear()

          // FIXME: m/z tolerance should be provided or configured
          // TODO: perform the m/z calibration as an external procedure (specific CLI command)
          // TODO: also search for 445.12 + H+?
          // FIXME: shall we find a way to calibrate the different WiSIM sub-spectra?
          val nearestPeak = mergedSpectrum.getNearestPeak(445.12003, 15)
          if (nearestPeak != null) {
            mzCalibratorBuffer += nearestPeak.getMz()
          }

          val finalSpectrum = if (mzCalibratorBuffer.size == mzCalibratorBuffer.maxSize) {
            val refMz = mzCalibratorBuffer.last
            val refMzError = refMz - 445.12003
            val refMzErrorPPM = 1e6 * refMzError / 445.12003
            //println("m/z error in PPM of 445.12 ion="+refMzErrorPPM)

            val specData = mergedSpectrum.data

            val calibrationMzShift = if (prevCalibratedSpectrum == null) refMzError
            else {
              val peaks = mergedSpectrum.data.toPeaks(mergedSpectrum.getHeader)
              val topPeaks = peaks.sortBy(p => -p.getIntensity()).take(10)

              val topPeaksMzDiffs = new ArrayBuffer[Double](10)
              for (topPeak <- topPeaks) {
                val foundPeak = prevCalibratedSpectrum.getNearestPeak(topPeak.getMz(), 15)
                if (foundPeak != null) {
                  topPeaksMzDiffs += (foundPeak.getMz() - topPeak.getMz()) / topPeak.getMz()
                }
              }

              if (topPeaksMzDiffs.isEmpty) refMzError
              else {
                val medianCalibMz = mzCalibratorBuffer.median()
                val medianCalibMzError = medianCalibMz - 445.12003
                val medianCalibMzErrorPPM = 1e6 * medianCalibMzError / 445.12003
                //println("Median m/z error in PPM of 445.12 ion="+medianCalibMzErrorPPM)

                val topPeaksMzErrorPPM = 1e6 * com.github.mzdb4s.util.math.median(topPeaksMzDiffs)
                //println("Median m/z diff in PPM between consecutive calibrated spectra="+topPeaksMzErrorPPM)

                if (math.abs(topPeaksMzErrorPPM + medianCalibMzErrorPPM) < math.abs(topPeaksMzErrorPPM + refMzErrorPPM)) {
                  medianCalibMzError
                } else {
                  refMzError
                }
              }
            }

            val calibratedSpecData = new SpectrumData(
              specData.mzList.map(mz => mz - calibrationMzShift),
              specData.intensityList
            )

            mergedSpectrum.copy(data = calibratedSpecData)
          } else {
            mergedSpectrum
          }

          //prevSpectrum = mergedSpectrum
          prevCalibratedSpectrum = finalSpectrum

          // Insert merged MS1 spectrum
          mzDbWriter.setInsertedSpectraCount(finalSpectrum.header.id.toInt - 1)
          mzDbWriter.insertSpectrum(finalSpectrum, specXmlMetaData, dataEncoding)
        }

        // Insert MS/MS spectrum
        mzDbWriter.setInsertedSpectraCount(spectrum.header.id.toInt - 1)
        mzDbWriter.insertSpectrum(mzDbSpectrum, spectrum.xmlMetaData, dataEncoding)
      }

      if (spectrum.header.id % 1000 == 0) {
        logger.debug(s"Processed ${spectrum.header.id} spectra...")
      }

      true
    }

    // Insert last MS1 spectrum
    if (spectraBuffer.nonEmpty) {
      val (mergedSpectrum, specXmlMetaData) = _mergeWiSIMSpectra(mzDbWriter, spectraBuffer, dataEncoding)
      spectraBuffer.clear()

      mzDbWriter.setInsertedSpectraCount(mergedSpectrum.header.id.toInt - 1)
      mzDbWriter.insertSpectrum(mergedSpectrum, specXmlMetaData, dataEncoding)
    }

    logger.debug("Closing mzDB writer...")
    mzDbWriter.close()
  }

  private def _mergeWiSIMSpectra(
    mzDbWriter: MzDbWriter,
    wiSimSpectra: ArrayBuffer[(Spectrum, SpectrumXmlMetaData)],
    dataEncoding: DataEncoding
  ): (com.github.mzdb4s.msdata.Spectrum, SpectrumXmlMetaData) = {

    var totalDataPointsCount = 0
    val scanWindows = new ArrayBuffer[(Float, Float)](wiSimSpectra.length)
    for ((wiSimSpectrum, _xmlMetaData) <- wiSimSpectra) {
      totalDataPointsCount += wiSimSpectrum.data.getPeaksCount()

      val scanWindow = wiSimSpectrum.header.getScanList.getScans().head.getScanWindowList().getScanWindows().head
      val scanMinMz = scanWindow.getCVParam(PsiMsCV.SCAN_WINDOW_LOWER_LIMIT).value.toFloat
      val scanMaxMz = scanWindow.getCVParam(PsiMsCV.SCAN_WINDOW_UPPER_LIMIT).value.toFloat
      scanWindows += (scanMinMz -> scanMaxMz)
    }

    val averagedLimits = scanWindows.sliding(3, 1).map({ scanWindowTuple =>
      val averageLowerLimit = (scanWindowTuple(0)._2 + scanWindowTuple(1)._1) / 2
      val averageUpperLimit = (scanWindowTuple(1)._2 + scanWindowTuple(2)._1) / 2
      (averageLowerLimit, averageUpperLimit)
    }).toArray

    val adjustedWindows = new ArrayBuffer[(Float, Float)](wiSimSpectra.length)
    adjustedWindows += (scanWindows.head._1 -> averagedLimits.head._1)
    adjustedWindows ++= averagedLimits
    adjustedWindows += (averagedLimits.last._2 -> scanWindows.last._2)

    // FIXME: we should verify that we don't keep peaks having exactly the same m/z (peak redundancy < few PPMs)
    val mergedMzList = new ArrayBuffer[Double](totalDataPointsCount)
    val mergedIntensityList = new ArrayBuffer[Float](totalDataPointsCount)

    for ((wiSimSpectrum, adjustedWindow) <- wiSimSpectra.zip(adjustedWindows)) {
      val mzList = wiSimSpectrum._1.data.mzList
      val intensityList = wiSimSpectrum._1.data.intensityList
      val retainedPeaks = mzList.zip(intensityList).filter(peak => peak._1 >= adjustedWindow._1 && peak._1 < adjustedWindow._2)
      val (retainedMzList, retainedIntensityList) = retainedPeaks.unzip
      mergedMzList ++= retainedMzList
      mergedIntensityList ++= retainedIntensityList
    }

    // FIXME: merge spectra meta-data to persist more consistent information
    val (firstWiSimSpectrum, firstXmlMetaData) = wiSimSpectra.head
    val mzDbSpectrumHeader = firstWiSimSpectrum.header.copy(id = firstWiSimSpectrum.header.id.toInt)
    val mergedSpectrumData = new SpectrumData(mergedMzList, mergedIntensityList)
    val mzDbSpectrum = com.github.mzdb4s.msdata.Spectrum(mzDbSpectrumHeader, mergedSpectrumData)

    (mzDbSpectrum, firstXmlMetaData)
  }

  protected def _tdf2mzdb(
    tdfDir: String,
    mzDbPath: String,
    config: Option[TimsDataReaderConfig]
  ): Unit = {

    import com.github.mzdb4s.io.timsdata._
    import com.github.sqlite4s.SQLiteFactory

    logger.info("--- TDF to mzDB file converter ---")

    try {
      require(new File(tdfDir).isDirectory, s"can't find a directory at: $tdfDir")
      require(new File(tdfDir,"analysis.tdf").isFile, s"can't find an 'analysis.tdf' file in the directory: $tdfDir")

      val mzDbFile = new File(mzDbPath)
      require(!mzDbFile.exists(), s"can't create mzDB file because it already exists at: ${mzDbFile.getAbsolutePath}")

      logger.info(s"Writing mzDB file located at: ${mzDbFile.getAbsolutePath}")

      implicit val sf = SQLiteFactory
      implicit val reader = TimsDataReader

      TimsReaderLibraryFactory.initLogger()

      val converter = new TimsData2MzDb(tdfDir, mzDbPath, config)
      converter.convert()

      logger.info("TDF -> mzDB conversion has completed :)")

      System.exit(0)
    } catch {
      case t: Throwable => {
        logger.error("TDF -> mzDB conversion has failed :(", t)
        System.exit(1)
      }
    }
  }

}


import scala.collection.mutable.Queue

class RingBuffer[T: Fractional](val maxSize: Int) {

  private val buffer = Queue[T]()

  def +=(elem: T): Unit = {
    if (buffer.size >= maxSize) {
      buffer.dequeue()  // Remove the oldest element (the first one)
    }
    buffer.enqueue(elem)  // Add the new element to the end
  }

  // Calculate the average of the elements in the buffer
  def median(): T = {
    if (buffer.isEmpty) throw new ArithmeticException("Buffer is empty")
    com.github.mzdb4s.util.math.median(buffer)
  }

  def last: T = buffer.last

  def size: Int = buffer.size
}