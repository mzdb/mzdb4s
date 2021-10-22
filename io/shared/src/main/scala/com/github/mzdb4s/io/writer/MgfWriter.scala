package com.github.mzdb4s.io.writer

import java.io._

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LongMap
import com.github.mzdb4s.{Logging, MzDbReader}
import com.github.mzdb4s.db.model.params.Precursor
import com.github.mzdb4s.db.model.params.param.PsiMsCV
import com.github.mzdb4s.db.table.SpectrumTable
import com.github.mzdb4s.io.mgf._
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteQuery}
import com.github.sqlite4s.query.SQLiteRecord


/**
  * @author MDB
  */
object MgfWriter {

  private[writer] val LINE_SEPARATOR: String = System.getProperty("line.separator")

  private val titleQuery = "SELECT id, title FROM spectrum WHERE ms_level=?"

  private val mathUtils: IMathUtils = MathUtils

  def apply(
    mzDBFilePath: String,
    mgfFile: Option[String] = None,
    msLevel: Int = 2,
    precursorEstimator: Option[IPrecursorEstimator] = Some(DefaultPrecursorEstimator),
    intensityCutoff: Option[Float] = None,
    exportProlineTitle: Boolean = true
  )(implicit sf: ISQLiteFactory): MgfWriter = {
    new MgfWriter(
      mzDBFilePath,
      mgfFile.getOrElse(mzDBFilePath + ".mgf"),
      msLevel,
      precursorEstimator.getOrElse(DefaultPrecursorEstimator),
      intensityCutoff,
      exportProlineTitle
    )
  }


  trait IPrecursorEstimator {
    /**
      * Returns the precursor m/z value of the specified SpectrumHeader.
      *
      * @param mzDbReader : the MS2 SpectrumHeader
      * @param spectrumHeader : the mzdbReader considered
      * @return the precursor m/z value of the specified SpectrumHeader
      */
    def getPrecursorMz(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Double]

    def getPrecursorCharge(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Int]

    def getMethodName(): String
    def estimationMethod: PrecursorEstimationMethod.Value

    def requiresPrecursorList: Boolean
    def requiresScanList: Boolean
  }

  object PrecursorEstimationMethod extends Enumeration {
    val MAIN_PRECURSOR_MZ = Value("main precursor mz")
    val SELECTED_ION_MZ = Value("selected ion mz")
    val REFINED = Value("built-in refined precursor mz")
    val REFINED_THERMO = Value("Thermo refined precursor mz")
  }

  object PrecursorEstimator {
    import PrecursorEstimationMethod._

    def apply(estimationMethod: PrecursorEstimationMethod.Value, mzTolPPM: Option[Float] = None): IPrecursorEstimator = {
      estimationMethod match {
        case MAIN_PRECURSOR_MZ => DefaultPrecursorEstimator
        case SELECTED_ION_MZ => SelectedIonPrecursorEstimator
        case REFINED => new RefinedPrecursorEstimator(mzTolPPM.get)
        case REFINED_THERMO => RefinedThermoPrecursorEstimator
      }
    }
  }

  abstract class AsbtractPrecursorEstimator extends IPrecursorEstimator with Logging {

    def getMethodName(): String = estimationMethod.toString

    def getPrecursorCharge(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Int] = spectrumHeader.getPrecursorCharge
  }

  object DefaultPrecursorEstimator extends AsbtractPrecursorEstimator {

    val estimationMethod: PrecursorEstimationMethod.Value = PrecursorEstimationMethod.MAIN_PRECURSOR_MZ

    val requiresPrecursorList = false
    val requiresScanList = false

    def getPrecursorMz(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Double] = {
      spectrumHeader.precursorMz // main precursor mz
    }
  }

  object SelectedIonPrecursorEstimator extends AsbtractPrecursorEstimator {

    val estimationMethod: PrecursorEstimationMethod.Value = PrecursorEstimationMethod.SELECTED_ION_MZ

    val requiresPrecursorList = false
    val requiresScanList = false

    def getPrecursorMz(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Double] = {
      try {
        val precursor = spectrumHeader.getPrecursor
        precursor.parseFirstSelectedIonMz()
      } catch {
        case e: Exception => {
          this.logger.trace("Retrieving selected ion m/z value failed", e)
          None
        }
      }
    }
  }

  class RefinedPrecursorEstimator(
    mzTolPPM: Float
  ) extends AsbtractPrecursorEstimator {

    val estimationMethod: PrecursorEstimationMethod.Value = PrecursorEstimationMethod.REFINED

    val requiresPrecursorList = false
    val requiresScanList = false

    def getPrecursorMz(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Double] = {

      try {
        val precursor = spectrumHeader.getPrecursor
        precursor.parseFirstSelectedIonMz().orElse(spectrumHeader.precursorMz).flatMap { precMzApprox =>
          this.refinePrecMz(
            mzDbReader,
            precursor,
            precMzApprox,
            mzTolPPM,
            spectrumHeader.getElutionTime(),
            timeTol = 5
          )
        }
      } catch {
        case e: Exception => {
          this.logger.trace("Refining precursor m/z value failed", e)
          None
        }
      }
    }


    /**
      * Refines the provided target m/z value by looking at the nearest value in the survey.
      *
      * @param precMz
      * the precursor m/z value to refine
      * @return the refined precursor m/z value
      */
    protected def refinePrecMz(
      mzDbReader: MzDbReader,
      precursor: Precursor,
      precMz: Double,
      mzTolPPM: Double,
      time: Float,
      timeTol: Float
    ): Option[Double] = {
      val spectrumSlices = this._getSpectrumSlicesInIsolationWindow(mzDbReader, precursor, time, timeTol)
      if (spectrumSlices == null) return None

      val peaks = new ArrayBuffer[IPeak]()
      for (sl <- spectrumSlices) {
        val p = sl.getNearestPeak(precMz, mzTolPPM)
        if (p != null) {
          p.setLcContext(sl.getHeader)
          peaks += p
        }
      }

      // Take the median value of mz
      if (peaks.length <= 1)
        return peaks.headOption.map(_.getMz)

      val mzSortedPeaks = peaks.sortBy(_.getMz)

      val n = peaks.size

      val medianMz = if (n % 2 != 0) mzSortedPeaks(n / 2).getMz
      else (mzSortedPeaks(n / 2 - 1).getMz + mzSortedPeaks(n / 2).getMz) / 2.0

      Some(medianMz)
    }

    private def _getSpectrumSlicesInIsolationWindow(mzDbReader: MzDbReader, precursor: Precursor, time: Float, timeTol: Float): Array[SpectrumSlice] = { // do a XIC over isolation window
      val iw = precursor.getIsolationWindow()
      if (iw == null) return null

      val cvEntries = Array(PsiMsCV.ISOLATION_WINDOW_LOWER_OFFSET, PsiMsCV.ISOLATION_WINDOW_TARGET_MZ, PsiMsCV.ISOLATION_WINDOW_UPPER_OFFSET)
      val cvParams = iw.getCVParams(cvEntries)
      val lowerMzOffset = cvParams(0).getValue.toFloat
      val targetMz = cvParams(1).getValue.toFloat
      val upperMzOffset = cvParams(2).getValue.toFloat
      val minmz = targetMz - lowerMzOffset
      val maxmz = targetMz + upperMzOffset
      val minrt = time - timeTol
      val maxrt = time + timeTol

      mzDbReader.getMsSpectrumSlices(minmz, maxmz, minrt, maxrt)
    }


    /*

    /**
      * Detects isotopic pattern in the survey and return the most probable mono-isotopic m/z value
      *
      * @param centerMz
      * the m/z value at the center of the isolation window
      * @return
      * @throws SQLiteException
      * @throws StreamCorruptedException
      */
    // TODO: it should be nice to perform this operation in mzdb-processing
    // This requires that the MgfWriter is be moved to this package
    protected def extractPrecMz(mzDbReader: MzDbReader, precursor: Precursor, precMz: Double, spectrumHeader: SpectrumHeader, timeTol: Float): Double = {
      val sid = spectrumHeader.getId
      val time = spectrumHeader.getTime
      // Do a XIC in the isolation window and around the provided time
      // FIXME: isolation window is not available for AbSciex files yet
      // final SpectrumSlice[] spectrumSlices = this._getSpectrumSlicesInIsolationWindow(precursor, time, timeTol);
      val spectrumSlices = mzDbReader.getMsSpectrumSlices(precMz - 1, precMz + 1, time - timeTol, time + timeTol)
      // TODO: perform the operation on all loaded spectrum slices ???
      var nearestSpectrumSlice = null
      for (sl <- spectrumSlices) {
        if (nearestSpectrumSlice == null) nearestSpectrumSlice = sl
        else if (Math.abs(sl.getHeader.getElutionTime - time) < Math.abs(nearestSpectrumSlice.getHeader.getElutionTime - time)) nearestSpectrumSlice = sl
      }
      val curPeak = nearestSpectrumSlice.getNearestPeak(precMz, mzTolPPM)
      if (curPeak == null) return precMz
      val previousPeaks = new util.ArrayList[Peak]
      var putativeZ = 2
      while ( {putativeZ <= 4}) { // avgIsoMassDiff = 1.0027
        val prevPeakMz = precMz + (1.0027 * -1 / putativeZ)
        val prevPeak = nearestSpectrumSlice.getNearestPeak(prevPeakMz, mzTolPPM)
        if (prevPeak != null) {
          prevPeak.setLcContext(nearestSpectrumSlice.getHeader)
          val prevPeakExpMz = prevPeak.getMz
          val approxZ = 1 / Math.abs(precMz - prevPeakExpMz)
          val approxMass = precMz * approxZ - approxZ * MsUtils.protonMass
          if (approxMass > 2000 && approxMass < 7000) { // TODO: find a solution for high mass values
            val minIntRatio = (1400.0 / approxMass).toFloat // inferred from lookup table
            val maxIntRatio = Math.min((2800.0 / approxMass).toFloat, 1)
            // Mass Min Max
            // 2000 0.7 1.4
            // 2500 0.56 1.12
            // 3000 0.47 0.93
            // 3500 0.4 0.8
            // 4000 0.35 0.7
            // 4500 0.31 0.62
            // 5000 0.28 0.56
            // 6000 0.23 0.47
            // 7000 0.2 0.4
            // Check if intensity ratio is valid (in the expected theoretical range)
            // TODO: analyze the full isotope pattern
            val intRatio = prevPeak.getIntensity / curPeak.getIntensity
            if (intRatio > minIntRatio && intRatio < maxIntRatio) { // Check if there is no next peak with a different charge state that could explain
              // this previous peak
              var foundInterferencePeak = false
              var interferencePeakMz = 0.0
              var interferenceZ = 1
              while ( {interferenceZ <= 6}) {
                if (interferenceZ != putativeZ) {
                  interferencePeakMz = prevPeakExpMz + (1.0027 * +1 / interferenceZ)
                  val interferencePeak = nearestSpectrumSlice.getNearestPeak(interferencePeakMz, mzTolPPM)
                  // If there is no defined peak with higher intensity
                  if (interferencePeak != null && interferencePeak.getIntensity > prevPeak.getIntensity) {
                    foundInterferencePeak = true
                    break //todo: break is not supported
                  }
                }

                {interferenceZ += 1; interferenceZ - 1}
              }
              if (foundInterferencePeak == false) {
                logger.debug("Found better m/z value for precMz=" + precMz + " at spectrum id=" + sid + " with int ratio=" + intRatio + " and z=" + putativeZ + " : " + prevPeakExpMz)
                previousPeaks.add(prevPeak)
              }
              else logger.debug("Found interference m/z value for precMz=" + precMz + " at spectrum id=" + sid + " : " + interferencePeakMz)
            }
          }
        }

        {putativeZ += 1; putativeZ - 1}
      }
      val nbPrevPeaks = previousPeaks.size
      if (nbPrevPeaks == 0) return precMz
      Collections.sort(previousPeaks, Peak.getIntensityComp)
      val mostIntensePrevPeak = previousPeaks.get(previousPeaks.size - 1)
      mostIntensePrevPeak.getMz
    }

     */
  }

  object RefinedThermoPrecursorEstimator extends AsbtractPrecursorEstimator {

    val estimationMethod: PrecursorEstimationMethod.Value = PrecursorEstimationMethod.REFINED_THERMO

    val requiresPrecursorList = false
    val requiresScanList = true

    def getPrecursorMz(mzDbReader: MzDbReader, spectrumHeader: SpectrumHeader): Option[Double] = {
      try {
        val precursor = spectrumHeader.getPrecursor
        precursor.parseFirstSelectedIonMz()

        val scanList = spectrumHeader.getOrLoadScanList()(mzDbReader.getMzDbContext())

        val monoMzOpt = scanList.getScans().headOption.flatMap { firstScan =>
          firstScan.getUserParams().find { userParam =>
            userParam.getName == "[Thermo Trailer Extra]Monoisotopic M/Z:"
          } map { precMzParam =>
            precMzParam.getValue.toDouble
          }
        }

        monoMzOpt

      } catch {
        case e: Exception => {
          this.logger.trace("Refined thermo m/z value retrieval failed", e)
          None
        }
      }
    }
  }


  /*def createMgfHeader(title: String, precMz: Double, charge: Int) {
    this(
      Seq(
        MgfHeaderEntry(MgfField.TITLE, title),
        // FIXME: use the trailer corresponding to the acquisition polarity (see mzDB meta-data)
        MgfHeaderEntry(MgfField.PEPMASS, round(precMz,4)),
        MgfHeaderEntry(MgfField.CHARGE, charge, "+")
      )
    )
  }*/

  def createMgfHeader(title: String, precMz: Double, rt: Float, scanNumber: Int): MgfHeader = {
    MgfHeader(
      Seq(
        MgfHeaderEntry(MgfField.TITLE, title),
        MgfHeaderEntry(MgfField.PEPMASS, mathUtils.round(precMz,4)),
        MgfHeaderEntry(MgfField.RTINSECONDS, mathUtils.round(rt,3)),
        MgfHeaderEntry(MgfField.SCANS, scanNumber)
      )
    )
  }

  def createMgfHeader(title: String, precMz: Double, charge: Int, rt: Float, scanNumber: Int): MgfHeader = {
    MgfHeader(
      Seq(
        MgfHeaderEntry(MgfField.TITLE, title),
        MgfHeaderEntry(MgfField.PEPMASS, mathUtils.round(precMz,4)),
        MgfHeaderEntry(MgfField.CHARGE, charge, Some("+")),
        MgfHeaderEntry(MgfField.RTINSECONDS, mathUtils.round(rt,2)),
        MgfHeaderEntry(MgfField.SCANS, scanNumber)
      )
    )
  }
}

class MgfWriter(
  val mzDBFilePath: String,
  val mgfFile: String,
  val msLevel: Int,
  val precEstimator: MgfWriter.IPrecursorEstimator,
  val intensityCutoff: Option[Float],
  val exportProlineTitle: Boolean
)(implicit sf: ISQLiteFactory) extends Logging {

  require(msLevel == 2 || msLevel == 3, "msLevel must be 2 or 3")

  private val spectrumSerializer: AbstractMgfSpectrumSerializer = new MgfSpectrumSerializer()

  private def _loadTitleBySpectrumId(mzDbReader: MzDbReader): collection.Map[Long, String] = {

    val titleBySpectrumId = new LongMap[String]()

    /** Higher order function processing SQL resulting records */
    val titleByIdFiller: (SQLiteRecord, Int) => Unit = { (elem: SQLiteRecord, idx: Int) =>
      val id = elem.columnInt(SpectrumTable.ID)
      val title = elem.columnString(SpectrumTable.TITLE)
      titleBySpectrumId.put(id, title)
    }

    new SQLiteQuery(mzDbReader.getConnection(), MgfWriter.titleQuery).bind(1, this.msLevel).forEachRecord(titleByIdFiller)

    titleBySpectrumId
  }

  def write(): Unit = {

    var precsNotFoundCount = 0

    // Create MGF writer and mzDB reader
    val mzDbReader = new MzDbReader(this.mzDBFilePath, true)

    val bufferSize = 4 * 1024 * 1024 // 4 MB buffer
    val mgfWriter = new BufferedOutputStream(new FileOutputStream(mgfFile), bufferSize)

    try {
      // Configure the mzDbReader in order to load all precursor lists and all scan list when reading spectra headers
      if (precEstimator.requiresPrecursorList) mzDbReader.enablePrecursorListLoading()
      if (precEstimator.requiresScanList) mzDbReader.enableScanListLoading()

      val titleBySpectrumId = if (!exportProlineTitle) LongMap.empty[String]
      else {
        val titles = this._loadTitleBySpectrumId(mzDbReader)
        this.logger.info("Number of loaded spectra titles: " + titles.size)
        titles
      }

      // Iterate MSn spectra
      val spectrumIterator = mzDbReader.getSpectrumIterator(2)

      val dataEncodingBySpectrumId = mzDbReader.getDataEncodingBySpectrumId()

      var spectraCount = 0
      while (spectrumIterator.hasNext()) {
        val s = spectrumIterator.next()
        val spectrumHeader = s.getHeader
        val spectrumId = spectrumHeader.getId
        val dataEnc = dataEncodingBySpectrumId(spectrumId)

        val charge = precEstimator.getPrecursorCharge(mzDbReader, spectrumHeader).getOrElse(0)
        val precMzOpt = precEstimator.getPrecursorMz(mzDbReader, spectrumHeader)
        val precMz = if (precMzOpt.isDefined) precMzOpt.get
        else {
          if (canLogTrace) logger.trace(s"precursor m/z could not be estimated using method (${precEstimator.getMethodName()}): falling back to default method")

          val mzOpt = MgfWriter.DefaultPrecursorEstimator.getPrecursorMz(mzDbReader, spectrumHeader)
          if (mzOpt.isDefined && mzOpt.get > 0.0) mzOpt.get
          else {
            precsNotFoundCount += 1
            0.0
          }
        }

        val spectrumAsBytes = spectrumSerializer.stringifySpectrum(
          mzDBFilePath,
          mzDbReader,
          s,
          titleBySpectrumId,
          dataEnc,
          precMz,
          charge,
          intensityCutoff,
          exportProlineTitle
        )
        //this.logger.debug("Writing spectrum with ID="+spectrumId);

        // Write the spectrum
        mgfWriter.write(spectrumAsBytes)

        spectraCount += 1


        if (spectraCount % 10000 == 0) {
          logger.debug(s"Processed $spectraCount spectra...")
        }
      }

      this.logger.info(s"MGF file successfully created: $spectraCount spectra exported")
      this.logger.info(s"Number of missing precursors: $precsNotFoundCount")

    } finally {
      mzDbReader.close()
      mgfWriter.flush()
      mgfWriter.close()

      spectrumSerializer.dispose()
    }

  }

}