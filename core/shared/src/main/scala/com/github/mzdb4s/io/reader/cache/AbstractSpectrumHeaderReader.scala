package com.github.mzdb4s.io.reader.cache

import scala.collection.mutable.{ArrayBuffer, LongMap}
import com.github.mzdb4s.AbstractMzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.MzDbReaderQueries
import com.github.mzdb4s.io.reader.param.ParamTreeParser
import com.github.mzdb4s.msdata.{ActivationType, PeakEncoding, SpectrumHeader}
import com.github.sqlite4s.ISQLiteRecordExtraction
import com.github.sqlite4s.query.SQLiteRecord


/**
  * @author David Bouyssie
  *
  */
object AbstractSpectrumHeaderReader {

  /** The time index width. */
  protected val TIME_INDEX_WIDTH = 15

  // Define some variable for spectrum header extraction
  private val _spectrumHeaderQueryStr = "SELECT id, initial_id, title, cycle, time, ms_level, activation_type, tic, base_peak_mz, base_peak_intensity, main_precursor_mz, main_precursor_charge, data_points_count, param_tree, scan_list, precursor_list, data_encoding_id, bb_first_spectrum_id FROM spectrum"
  private val _ms1SpectrumHeaderQueryStr = _spectrumHeaderQueryStr + " WHERE ms_level = 1"
  private val _ms2SpectrumHeaderQueryStr = _spectrumHeaderQueryStr + " WHERE ms_level = 2"
  private val _ms3SpectrumHeaderQueryStr = _spectrumHeaderQueryStr + " WHERE ms_level = 3"

  protected object SpectrumHeaderCol extends Enumeration {
    val ID = Value("id")
    val INITIAL_ID = Value("initial_id")
    val TITLE = Value("title")
    val CYCLE = Value("cycle")
    val TIME = Value("time")
    val MS_LEVEL = Value("ms_level")
    val ACTIVATION_TYPE = Value("activation_type")
    val TIC = Value("tic")
    val BASE_PEAK_MZ = Value("base_peak_mz")
    val BASE_PEAK_INTENSITY = Value("base_peak_intensity")
    val MAIN_PRECURSOR_MZ = Value("main_precursor_mz")
    val MAIN_PRECURSOR_CHARGE = Value("main_precursor_charge")
    val DATA_POINTS_COUNT = Value("data_points_count")
    val PARAM_TREE = Value("param_tree")
    val SCAN_LIST = Value("scan_list")
    val PRECURSOR_LIST = Value("precursor_list")
    val DATA_ENCODING_ID = Value("data_encoding_id")
    val BB_FIRST_SPECTRUM_ID = Value("bb_first_spectrum_id")

    /*@SuppressWarnings(Array("unused"))
    protected val columnName: String = nulldef
    this (colName: String) {
      this ()
      this.columnName = colName
    }*/
  }

  protected object SpectrumHeaderColIdx {
    import SpectrumHeaderCol._
    // FIXME: check that enum ids are the right indexes
    private[cache] val id = ID.id
    private[cache] val initialId = INITIAL_ID.id
    private[cache] val title = TITLE.id
    private[cache] val cycle = CYCLE.id
    private[cache] val time = TIME.id
    private[cache] val msLevel = MS_LEVEL.id
    private[cache] val activationType = ACTIVATION_TYPE.id
    private[cache] val tic = TIC.id
    private[cache] val basePeakMz = BASE_PEAK_MZ.id
    private[cache] val basePeakIntensity = BASE_PEAK_INTENSITY.id
    private[cache] val mainPrecursorMz = MAIN_PRECURSOR_MZ.id
    private[cache] val mainPrecursorCharge = MAIN_PRECURSOR_CHARGE.id
    private[cache] val dataPointsCount = DATA_POINTS_COUNT.id
    private[cache] val paramTree = PARAM_TREE.id
    private[cache] val scanList = SCAN_LIST.id
    private[cache] val precursorList = PRECURSOR_LIST.id
    private[cache] val dataEncodingId = DATA_ENCODING_ID.id
    private[cache] val bbFirstSpectrumId = BB_FIRST_SPECTRUM_ID.id
  }

}

abstract class AbstractSpectrumHeaderReader(
  mzDbReader: AbstractMzDbReader,
  dataEncodingReader: AbstractDataEncodingReader
) extends IMzDbEntityCacheContainer {

  import AbstractSpectrumHeaderReader._

  val entityCache: Option[MzDbEntityCache] = mzDbReader.getEntityCache()

  private var _spectrumHeaderExtractor: ISQLiteRecordExtraction[SpectrumHeader] = _

  private def _getSpectrumHeaderExtractor()(implicit mzDbCtx: MzDbContext): ISQLiteRecordExtraction[SpectrumHeader] = {
    if (_spectrumHeaderExtractor != null) return _spectrumHeaderExtractor

    _spectrumHeaderExtractor = new Object() with ISQLiteRecordExtraction[SpectrumHeader] {
      def extractRecord(record: SQLiteRecord): SpectrumHeader = {
        val stmt = record.getStatement
        val msLevel = stmt.columnInt(SpectrumHeaderColIdx.msLevel)
        val activationTypeOpt = if (msLevel == 1) None
        else {
          val activationTypeAsStr = stmt.columnString(SpectrumHeaderColIdx.activationType)
          Some(ActivationType.withName(activationTypeAsStr))
        }

        var precursorMz = Option.empty[Double]
        var precursorCharge = Option.empty[Int]
        if (msLevel >= 2) {
          precursorMz = Some(stmt.columnDouble(SpectrumHeaderColIdx.mainPrecursorMz))
          precursorCharge = Some(stmt.columnInt(SpectrumHeaderColIdx.mainPrecursorCharge))
        }

        val bbFirstSpectrumId = stmt.columnInt(SpectrumHeaderColIdx.bbFirstSpectrumId)
        val dataEnc = dataEncodingReader.getDataEncoding(stmt.columnInt(SpectrumHeaderColIdx.dataEncodingId))
        val isHighRes = if (dataEnc.getPeakEncoding == PeakEncoding.LOW_RES_PEAK) false
        else true

        val sh = new SpectrumHeader(
          stmt.columnLong(SpectrumHeaderColIdx.id),
          stmt.columnInt(SpectrumHeaderColIdx.initialId),
          stmt.columnString(SpectrumHeaderColIdx.title),
          stmt.columnInt(SpectrumHeaderColIdx.cycle),
          stmt.columnDouble(SpectrumHeaderColIdx.time).toFloat,
          msLevel,
          activationTypeOpt,
          stmt.columnInt(SpectrumHeaderColIdx.dataPointsCount),
          isHighRes,
          stmt.columnDouble(SpectrumHeaderColIdx.tic).toFloat,
          stmt.columnDouble(SpectrumHeaderColIdx.basePeakMz),
          stmt.columnDouble(SpectrumHeaderColIdx.basePeakIntensity).toFloat,
          precursorMz,
          precursorCharge,
          bbFirstSpectrumId
        )

        if (mzDbReader.isParamTreeLoadingEnabled)
          sh.setParamTree(ParamTreeParser.parseParamTree(stmt.columnString(AbstractSpectrumHeaderReader.SpectrumHeaderColIdx.paramTree)))

        if (mzDbReader.isScanListLoadingEnabled)
          sh.setScanList(ParamTreeParser.parseScanList(stmt.columnString(AbstractSpectrumHeaderReader.SpectrumHeaderColIdx.scanList)))

        if (mzDbReader.isPrecursorListLoadingEnabled && msLevel >= 2)
          sh.setPrecursor(ParamTreeParser.parsePrecursor(stmt.columnString(AbstractSpectrumHeaderReader.SpectrumHeaderColIdx.precursorList)))

        sh
      }
    }

    _spectrumHeaderExtractor
  }

  private def _loadSpectrumHeaders(msLevel: Int, queryStr: String)(implicit mzDbCtx: MzDbContext): Array[SpectrumHeader] = {
    val spectraCount = MzDbReaderQueries.getSpectraCount(msLevel)
    val spectrumHeaders = new Array[SpectrumHeader](spectraCount)
    mzDbCtx.newSQLiteQuery(queryStr).extractRecords(this._getSpectrumHeaderExtractor(), spectrumHeaders)
    spectrumHeaders
  }

  private def _buildSpectrumHeaderById(spectrumHeaders: Array[SpectrumHeader]): LongMap[SpectrumHeader] = {
    val spectrumHeaderById = new LongMap[SpectrumHeader](spectrumHeaders.length)

    for (spectrumHeader <- spectrumHeaders)
      spectrumHeaderById.put(spectrumHeader.getId, spectrumHeader)

    spectrumHeaderById
  }

  protected def getSpectrumHeaders()(implicit mzDbCtx: MzDbContext): Array[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && entityCacheOrNull.spectrumHeaders != null) this.entityCacheOrNull.spectrumHeaders
    else {
      val ms1SpectrumHeaders = this.getMs1SpectrumHeaders()
      val ms2SpectrumHeaders = this.getMs2SpectrumHeaders()
      val ms3SpectrumHeaders = this.getMs3SpectrumHeaders()
      val spectraCount = ms1SpectrumHeaders.length + ms2SpectrumHeaders.length + ms3SpectrumHeaders.length
      val spectrumHeaders = new Array[SpectrumHeader](spectraCount)
      System.arraycopy(ms1SpectrumHeaders, 0, spectrumHeaders, 0, ms1SpectrumHeaders.length)
      System.arraycopy(ms2SpectrumHeaders, 0, spectrumHeaders, ms1SpectrumHeaders.length, ms2SpectrumHeaders.length)
      System.arraycopy(ms3SpectrumHeaders, 0, spectrumHeaders, ms1SpectrumHeaders.length + ms2SpectrumHeaders.length, ms3SpectrumHeaders.length)
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.spectrumHeaders = spectrumHeaders
      spectrumHeaders
    }
  }

  def getSpectrumHeaderById()(implicit mzDbCtx: MzDbContext): LongMap[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.spectrumHeaderById != null) this.entityCacheOrNull.spectrumHeaderById
    else {
      val spectrumHeaderById = _buildSpectrumHeaderById(this.getSpectrumHeaders())
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.spectrumHeaderById = spectrumHeaderById
      spectrumHeaderById
    }
  }

  protected def getMs1SpectrumHeaders()(implicit mzDbCtx: MzDbContext): Array[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms1SpectrumHeaders != null) this.entityCacheOrNull.ms1SpectrumHeaders
    else {
      val ms1SpectrumHeaders = _loadSpectrumHeaders(1, AbstractSpectrumHeaderReader._ms1SpectrumHeaderQueryStr)
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms1SpectrumHeaders = ms1SpectrumHeaders
      ms1SpectrumHeaders
    }
  }

  def getMs1SpectrumHeaderById()(implicit mzDbCtx: MzDbContext): LongMap[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms1SpectrumHeaderById != null) this.entityCacheOrNull.ms1SpectrumHeaderById
    else {
      val ms1SpectrumHeaderById = _buildSpectrumHeaderById(this.getMs1SpectrumHeaders())
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms1SpectrumHeaderById = ms1SpectrumHeaderById
      ms1SpectrumHeaderById
    }
  }

  protected def getMs2SpectrumHeaders()(implicit mzDbCtx: MzDbContext): Array[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms2SpectrumHeaders != null) this.entityCacheOrNull.ms2SpectrumHeaders
    else {
      val ms2SpectrumHeaders = _loadSpectrumHeaders(2, AbstractSpectrumHeaderReader._ms2SpectrumHeaderQueryStr)
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms2SpectrumHeaders = ms2SpectrumHeaders
      ms2SpectrumHeaders
    }
  }

  def getMs2SpectrumHeaderById()(implicit mzDbCtx: MzDbContext): LongMap[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms2SpectrumHeaderById != null) this.entityCacheOrNull.ms2SpectrumHeaderById
    else {
      val ms2SpectrumHeaderById = _buildSpectrumHeaderById(this.getMs2SpectrumHeaders())
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms2SpectrumHeaderById = ms2SpectrumHeaderById
      ms2SpectrumHeaderById
    }
  }

  protected def getMs3SpectrumHeaders()(implicit mzDbCtx: MzDbContext): Array[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms3SpectrumHeaders != null) this.entityCacheOrNull.ms3SpectrumHeaders
    else {
      val ms3SpectrumHeaders = _loadSpectrumHeaders(3, AbstractSpectrumHeaderReader._ms3SpectrumHeaderQueryStr)
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms3SpectrumHeaders = ms3SpectrumHeaders
      ms3SpectrumHeaders
    }
  }

  def getMs3SpectrumHeaderById()(implicit mzDbCtx: MzDbContext): LongMap[SpectrumHeader] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.ms3SpectrumHeaderById != null) this.entityCacheOrNull.ms3SpectrumHeaderById
    else {
      val ms3SpectrumHeaderById = _buildSpectrumHeaderById(this.getMs3SpectrumHeaders())
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.ms3SpectrumHeaderById = ms3SpectrumHeaderById
      ms3SpectrumHeaderById
    }
  }

  def getSpectrumHeader(id: Long)(implicit mzDbCtx: MzDbContext): SpectrumHeader = {
    if (this.entityCache.nonEmpty) this.getSpectrumHeaderById().apply(id)
    else {
      val queryStr = _spectrumHeaderQueryStr + " WHERE id = ? "
      mzDbCtx.newSQLiteQuery(queryStr).bind(1, id).extractRecord(this._spectrumHeaderExtractor)
    }
  }

  protected def getSpectrumTimeById()(implicit mzDbCtx: MzDbContext): LongMap[Float] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.spectrumTimeById != null) this.entityCacheOrNull.spectrumTimeById
    else {
      val spectraCount = MzDbReaderQueries.getSpectraCount()
      val spectrumTimes = mzDbCtx.newSQLiteQuery("SELECT time FROM spectrum").extractFloats(spectraCount)
      assert(
        spectraCount == spectrumTimes.length,
        "spectraCount != spectrumTimes.length"
      )

      val spectrumTimeById = new LongMap[Float]
      // TODO: check this approach is not too dangerous
      // TODO: load the pair of values in the SQL query???
      var spectrumId = 0
      for (spectrumTime <- spectrumTimes) {
        spectrumId += 1
        spectrumTimeById.put(spectrumId, spectrumTime)
      }

      if (this.entityCache.nonEmpty) this.entityCacheOrNull.spectrumTimeById = spectrumTimeById

      spectrumTimeById
    }
  }

  protected def getSpectrumHeaderForTime(time: Float, msLevel: Int)(implicit mzDbCtx: MzDbContext): SpectrumHeader = {
    if (this.entityCache.nonEmpty) {
      val spectrumIdsByTimeIndex = this._getSpectrumIdsByTimeIndex()
      val timeIndex = (time / TIME_INDEX_WIDTH).toInt

      var nearestSpectrumHeader: SpectrumHeader = null

      var index = timeIndex - 1
      while (index <= timeIndex + 1) {
        if (spectrumIdsByTimeIndex.contains(index)) {
          val tmpSpectrumIds = spectrumIdsByTimeIndex(index)
          //import scala.collection.JavaConversions._
          for (tmpSpectrumId <- tmpSpectrumIds) {
            val spectrumH = this.getSpectrumHeader(tmpSpectrumId)
            assert(spectrumH != null, s"can't retrieve spectrum with id =$tmpSpectrumId")

            if (spectrumH.getMsLevel == msLevel) {
              if (nearestSpectrumHeader == null || math.abs(spectrumH.getTime - time) < math.abs(nearestSpectrumHeader.getTime - time))
                nearestSpectrumHeader = spectrumH
            }
          }

          index += 1
        }
      }

      nearestSpectrumHeader

    } else {
      val queryStr = "SELECT id FROM spectrum WHERE ms_level = ? ORDER BY abs(spectrum.time - ?) ASC limit 1"
      val spectrumId = mzDbCtx.newSQLiteQuery(queryStr).bind(1, msLevel).bind(2, time).extractSingleInt()
      this.getSpectrumHeader(spectrumId)
      }
  }


  private def _getSpectrumIdsByTimeIndex()(implicit mzDbCtx: MzDbContext): LongMap[ArrayBuffer[Long]] = {
    if (this.entityCache.nonEmpty && this.entityCacheOrNull.spectrumIdsByTimeIndex != null)
      return this.entityCacheOrNull.spectrumIdsByTimeIndex

    val spectrumHeaders = this.getSpectrumHeaders()

    val spectrumIdsByTimeIndex = new LongMap[ArrayBuffer[Long]]
    for (spectrumH <- spectrumHeaders) {
      val timeIndex = (spectrumH.getTime / TIME_INDEX_WIDTH).toInt

      spectrumIdsByTimeIndex.getOrElseUpdate(timeIndex, new ArrayBuffer[Long]()) += spectrumH.getId
    }

    if (this.entityCache.nonEmpty)
      this.entityCacheOrNull.spectrumIdsByTimeIndex = spectrumIdsByTimeIndex

    spectrumIdsByTimeIndex
  }

  // TODO: use entity cache ?
  protected def getSpectrumIdsForTimeRange(minRT: Float, maxRT: Float, msLevel: Int)(implicit mzDbCtx: MzDbContext): Array[Long] = {
    val query = mzDbCtx.newSQLiteQuery("SELECT id FROM spectrum WHERE ms_level = ? AND time >= ? AND time <= ?")
    query.bind(1, msLevel).bind(2, minRT).bind(3, maxRT).extractLongs(1)
  }

}