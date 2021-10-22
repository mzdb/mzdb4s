package com.github.mzdb4s.io.writer

import java.io.File

import com.github.mzdb4s.Logging

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.LongMap

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params.ParamTree
import com.github.mzdb4s.db.model.params.param._
import com.github.mzdb4s.db.table._
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection._
import com.github.sqlite4s._

object AbstractMzDbWriter {
  val MODEL_VERSION = 0.7
}

/**
  * Allows to create a new mzDB file.
  *
  * @author David Bouyssie
  */
abstract class AbstractMzDbWriter extends Logging {

  // Required arguments
  val dbLocation: File
  val metaData: MzDbMetaData
  val bbSizes: BBSizes
  val isDIA: Boolean
  protected implicit val sf: ISQLiteFactory

  // Define some private objects
  private val _xmlSerializer = com.github.mzdb4s.io.serialization.XmlSerializer

  /** Some fields initialized inside the open() method **/
  private var _connection: ISQLiteConnection = _
  private var _bboxInsertStmt: ISQLiteStatement = _
  private var _rtreeInsertStmt: ISQLiteStatement = _
  private var _msnRtreeInsertStmt: ISQLiteStatement = _
  private var _spectrumInsertStmt: ISQLiteStatement = _

  private var _insertedSpectraCount = 0
  //private val _spectrumIdByTime = new HashMap[Float, Long]()

  private val _dataEncodingRegistry = new DataEncodingRegistry()
  private val _bbCache = new BoundingBoxWriterCache(bbSizes)
  // TODO: add support for multiple runs
  private val _runSliceStructureFactory = new RunSliceStructureFactory(1)

  protected def bboxInsertStmt: ISQLiteStatement = _bboxInsertStmt

  protected def formatDateToISO8601String(date: java.util.Date) : String

  /**
    * Gets the connection.
    *
    * @return the connection
    */
  def getConnection(): ISQLiteConnection = this._connection

  def open(): Unit = {
    this._connection = sf.newSQLiteConnection(dbLocation)
    this._connection.open(allowCreate = true) // allow create => true

    // SQLite optimization
    // See: https://blog.devart.com/increasing-sqlite-performance.html
    this._connection.exec("PRAGMA encoding='UTF-8';")
    this._connection.exec("PRAGMA synchronous=OFF;")
    this._connection.exec("PRAGMA journal_mode=OFF;")
    this._connection.exec("PRAGMA temp_store=2;")
    this._connection.exec("PRAGMA cache_size=-100000;") // around 100 Mo
    this._connection.exec("PRAGMA page_size=4096;") // see: https://www.sqlite.org/pgszchng2016.html

    this._connection.exec("PRAGMA automatic_index=OFF;")
    this._connection.exec("PRAGMA locking_mode=EXCLUSIVE;") // we want to lock file access for the whole creation process

    this._connection.exec("PRAGMA foreign_keys=OFF;") // FIXME: there is an issue with tmp_spectrum that need to be solved to enable this
    this._connection.exec("PRAGMA ignore_check_constraints=ON;") // to be a little bit faster (should be OFF in dev mode)

    // Create a temporary table containing a copy of the spectrum table
    // System.out.println("before CREATE TEMP TABLE");
    // connection.exec("CREATE TEMP TABLE tmp_spectrum AS SELECT * FROM spectrum");
    // System.out.println("after CREATE TEMP TABLE");
    // BEGIN TRANSACTION
    this._connection.exec("BEGIN TRANSACTION;")

    //println("auto commit:" + this._connection.getAutoCommit())

    // Init DDL schema
    this._connection.exec(MzDbSchema.getDDLString())

    // Init some INSERT statements //
    _bboxInsertStmt = _connection.prepare(s"INSERT INTO ${BoundingBoxTable.tableName} VALUES (NULL, ?, ?, ?, ?)", cached = false)

    _rtreeInsertStmt = _connection.prepare(s"INSERT INTO bounding_box_rtree VALUES (?, ?, ?, ?, ?)", cached = false)

    _msnRtreeInsertStmt = _connection.prepare(s"INSERT INTO bounding_box_msn_rtree VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", cached = false)

    val placeHolders = List.fill(24)("?").mkString(", ")
    _spectrumInsertStmt = _connection.prepare(s"INSERT INTO tmp_spectrum VALUES ($placeHolders)", cached = false)

    this._insertMetaData(metaData)
  }

  def close(): Unit = {
    if (this._connection == null) throw new IllegalStateException("The method open() must first be called")

    // Last INSERTs + mzDB INDEXES
    try {
      // FIXME: insert missing BBs (last entries in bbCache)
      this._bbCache.getBBRowsKeys().foreach { case (msLevel: Int,isoWinOpt: Option[IsolationWindow]) =>
        this._flushBBRow(msLevel, isoWinOpt)
      }

      // Persist TEMPORARY spectrum table
      _connection.exec("CREATE TABLE spectrum AS SELECT * FROM tmp_spectrum;")
      //connection.exec("INSERT INTO spectrum SELECT * FROM tmp_spectrum;")

      // --- INSERT DATA ENCODINGS --- //
      var stmt = _connection.prepare(s"INSERT INTO ${DataEncodingTable.tableName} VALUES (?, ?, ?, ?, ?, ?, NULL)", cached = false)
      val dataEncs = this._dataEncodingRegistry.getDistinctDataEncodings()
      for (dataEnc <- dataEncs) {
        var mzPrecision = 64
        var intPrecision = 32
        val peakEnc = dataEnc.getPeakEncoding

        if (peakEnc == PeakEncoding.LOW_RES_PEAK) mzPrecision = 32
        else if (peakEnc == PeakEncoding.NO_LOSS_PEAK) intPrecision = 64

        stmt.bind(1, dataEnc.getId)
        stmt.bind(2, dataEnc.getMode.toString)
        stmt.bind(3, dataEnc.getCompression)
        stmt.bind(4, dataEnc.getByteOrder.toString.toLowerCase)
        stmt.bind(5, mzPrecision)
        stmt.bind(6, intPrecision)
        stmt.step()
        stmt.reset()
      }
      stmt.dispose()

      // Finalize the creation of run slices
      stmt = _connection.prepare(s"INSERT INTO ${RunSliceTable.tableName} VALUES (?, ?, ?, ?, ?, NULL, ?)", cached = false)
      for (runSlice <- this._runSliceStructureFactory.getAllRunSlices()) {
        stmt.bind(1, runSlice.id)
        stmt.bind(2, runSlice.msLevel)
        stmt.bind(3, runSlice.number)
        stmt.bind(4, runSlice.beginMz)
        stmt.bind(5, runSlice.endMz)
        stmt.bind(6, runSlice.runId)

        stmt.step()
        stmt.reset()
      }
      stmt.dispose()

      // Create all indexes here
      this._connection.exec("CREATE UNIQUE INDEX spectrum_initial_id_idx ON spectrum (initial_id ASC,run_id ASC);")
      this._connection.exec("CREATE INDEX spectrum_ms_level_idx ON spectrum (ms_level ASC,run_id ASC);")
      this._connection.exec("CREATE UNIQUE INDEX run_name_idx ON run (name);")
      this._connection.exec("CREATE UNIQUE INDEX run_slice_mz_range_idx ON run_slice (begin_mz ASC,end_mz ASC,ms_level ASC,run_id ASC);")
      this._connection.exec("CREATE INDEX bounding_box_run_slice_idx ON bounding_box (run_slice_id ASC);")
      this._connection.exec("CREATE INDEX bounding_box_first_spectrum_idx ON bounding_box (first_spectrum_id ASC); ")
      this._connection.exec("CREATE UNIQUE INDEX controlled_vocabulary_full_name_idx ON cv (full_name);")
      this._connection.exec("CREATE INDEX controlled_vocabulary_uri_idx ON cv (uri);")
      this._connection.exec("CREATE UNIQUE INDEX source_file_name_idx ON source_file (name);")
      this._connection.exec("CREATE UNIQUE INDEX sample_name_idx ON sample (name);")
      this._connection.exec("CREATE UNIQUE INDEX software_name_idx ON software (name);")
      this._connection.exec("CREATE UNIQUE INDEX instrument_configuration_name_idx ON instrument_configuration (name);")
      this._connection.exec("CREATE UNIQUE INDEX processing_method_number_idx ON processing_method (number ASC);")
      this._connection.exec("CREATE UNIQUE INDEX data_processing_name_idx ON data_processing (name);")
      this._connection.exec("CREATE UNIQUE INDEX chromatogram_name_idx ON chromatogram (name);")
      this._connection.exec("CREATE UNIQUE INDEX cv_term_name_idx ON cv_term (name ASC);")
      this._connection.exec("CREATE UNIQUE INDEX user_term_name_idx ON user_term (name ASC);")
      this._connection.exec("CREATE UNIQUE INDEX cv_unit_name_idx ON cv_unit (name ASC);")
      this._connection.exec("CREATE INDEX spectrum_bb_first_spectrum_id_idx ON spectrum (bb_first_spectrum_id ASC);")

      // COMMIT TRANSACTION
      this._connection.exec("COMMIT TRANSACTION;")

    } finally {
      if (this._bboxInsertStmt != null) _bboxInsertStmt.dispose()
      if (this._rtreeInsertStmt != null) _rtreeInsertStmt.dispose()
      if (this._msnRtreeInsertStmt != null) _msnRtreeInsertStmt.dispose()
      if (this._spectrumInsertStmt != null) _spectrumInsertStmt.dispose()

      this._connection.dispose()

      if (!this._connection.isMemoryDatabase()) {
        // Update sqlite_sequence table using a fresh connection
        // DBO: I don't why but it doesn't work inside the previous connection
        this._connection = sf.newSQLiteConnection(dbLocation)
        this._connection.open(allowCreate = false)
        this._connection.exec(s"INSERT INTO sqlite_sequence VALUES ('spectrum',${_insertedSpectraCount});")
        this._connection.dispose()
      }

    }
  }

  private def _insertMetaData(metaData: MzDbMetaData): Unit = {
    if (this._connection == null) throw new IllegalStateException("The method open() must first be called")

    // --- INSERT DATA PROCESSINGS --- //
    var stmt = _connection.prepare(s"INSERT INTO ${DataProcessingTable.tableName} VALUES (NULL, ?)", cached = false)
    val procMethods = metaData.getProcessingMethods
    val dpNames = procMethods.map(_.dataProcessingName).distinct
    val dpIdByName = dpNames.map { dpName =>
      stmt.bind(1, dpName)
      stmt.step()
      val dpId = _connection.getLastInsertId()
      stmt.reset()
      dpName -> dpId
    }.toMap

    stmt.dispose()

    // --- INSERT PROCESSING METHODS --- //
    stmt = _connection.prepare(s"INSERT INTO ${ProcessingMethodTable.tableName} VALUES (NULL, ?, ?, ?, ?, ?)", cached = false)

    for (procMethod <- procMethods) {
      stmt.bind(1, procMethod.getNumber)
      if (procMethod.getParamTree().isEmpty) stmt.bindNull(2)
      else stmt.bind(2, _xmlSerializer.serializeParamTree(procMethod.getParamTree().get))
      stmt.bindNull(3)
      stmt.bind(4, dpIdByName(procMethod.getDataProcessingName))
      stmt.bind(5, procMethod.getSoftwareId)
      stmt.step()
      stmt.reset()
    }
    stmt.dispose()

    // --- INSERT SHARED PARAM TREES --- //
    stmt = _connection.prepare("INSERT INTO " + SharedParamTreeTable.tableName + " VALUES (NULL, ?, ?)", cached = false)
    stmt.bind(1, _xmlSerializer.serializeParamTree(metaData.getCommonInstrumentParams.getParamTree()))
    stmt.bind(2, "CommonInstrumentParams")
    stmt.step()
    stmt.reset()
    stmt.dispose()

    // --- INSERT INSTRUMENT CONFIGS --- //
    stmt = _connection.prepare("INSERT INTO " + InstrumentConfigurationTable.tableName + " VALUES (NULL, ?, NULL, ?, NULL,  ?)", cached = false)
    val instConfigs = metaData.getInstrumentConfigurations
    for (instConfig <- instConfigs; if instConfig.getComponentList != null) {
      stmt.bind(1, instConfig.getName)
      stmt.bind(2, _xmlSerializer.serializeComponentList(instConfig.getComponentList))
      stmt.bind(3, instConfig.getSoftwareId.getOrElse(1)) // FIXME: softwareId required in mzDB but not in mzML
      stmt.step()
      stmt.reset()
    }
    stmt.dispose()

    // --- INSERT MZDB HEADER --- //
    stmt = _connection.prepare(s"INSERT INTO ${MzdbTable.tableName} VALUES (?, ?, ?, ?, ?)", cached = false)

    val mzDbHeader = metaData.getMzDbHeader

    /*
    // SMALL TEMP HACK: change BB sizes because we currently store each spectrum in a single BB
    // FIXME: remove this hack
    var mzdbHeaderParams = mzDbHeader.getParamTree()
    val patchedUserParams = mzdbHeaderParams.getUserParams().map { userParam =>
      if (userParam.name == "ms1_bb_mz_width") userParam.copy(value = "10000")
      else if (userParam.name == "ms1_bb_time_width") userParam.copy(value = "0")
      else userParam
    }
    mzdbHeaderParams = mzdbHeaderParams.copy(userParams = patchedUserParams)
    */

    // Update BB sizes in params
    val mzdbHeaderParams = mzDbHeader.getParamTree().get
    /*paramTree.copy(userParams =
    )*/

    val bbSizesKeySet = Set("ms1_bb_mz_width","ms1_bb_time_width","msn_bb_mz_width","msn_bb_time_width")
    val userExtraParams = mzdbHeaderParams.getUserParams().filterNot( p =>
      bbSizesKeySet.contains(p.name)
    )
    val updatedUserParams = userExtraParams ++ List(
      UserParam(name="ms1_bb_mz_width", value = bbSizes.BB_MZ_HEIGHT_MS1.toString, `type` = "xsd:float"),
      UserParam(name="ms1_bb_time_width", value = bbSizes.BB_RT_WIDTH_MS1.toString, `type` = "xsd:float"),
      UserParam(name="msn_bb_mz_width", value = bbSizes.BB_MZ_HEIGHT_MSn.toString, `type` = "xsd:float"),
      UserParam(name="msn_bb_time_width", value = bbSizes.BB_RT_WIDTH_MSn.toString, `type` = "xsd:float")
    )

    /*val patchedUserParams = mzdbHeaderParams.getUserParams().map { userParam =>
      userParam.name match {
        case "ms1_bb_mz_width" => userParam.copy(value = bbSizes.BB_MZ_HEIGHT_MS1.toString)
        case "ms1_bb_time_width" => userParam.copy(value = bbSizes.BB_RT_WIDTH_MS1.toString)
        case "msn_bb_mz_width" => userParam.copy(value = bbSizes.BB_MZ_HEIGHT_MSn.toString)
        case "msn_bb_time_width" => userParam.copy(value = bbSizes.BB_MZ_HEIGHT_MSn.toString)
        case _ => userParam
      }
    }*/

    mzdbHeaderParams.setUserParams(updatedUserParams)

    stmt.bind(1, AbstractMzDbWriter.MODEL_VERSION)
    stmt.bind(2, (new java.util.Date().getTime / 1000).toInt)
    stmt.bind(3, _xmlSerializer.serializeFileContent(mzDbHeader.fileContent))
    stmt.bind(4, "") // FIXME: define contacts in the mzDB file

    val serializedMzDbParamTree = _xmlSerializer.serializeParamTree(mzdbHeaderParams)
    //println(serializedMzDbParamTree)
    stmt.bind(5, serializedMzDbParamTree)

    stmt.step()
    stmt.reset()
    stmt.dispose()

    // --- INSERT RUNS --- //
    stmt = _connection.prepare(s"INSERT INTO ${RunTable.tableName} VALUES (NULL,?,?,?,?,?,?,?,?,?)", cached = false)
    val runs = metaData.getRuns
    for (run <- runs) {
      stmt.bind(1, run.getName)
      stmt.bind(2, formatDateToISO8601String(run.getStartTimestamp))

      // Inject the 'acquisition parameter' CV param if it doesn't exist
      val runParamTree = run.getParamTree().getOrElse(ParamTree())
      val cvParams = runParamTree.getCVParams()
      if (!cvParams.exists(_.accession == PsiMsCV.ACQUISITION_PARAMETER.getAccession())) {
        // FIXME: implement better AcquisitionMode => replace isDIA by the AcquisitionMode
        val mode = if (isDIA) AcquisitionMode.SWATH.getName() else AcquisitionMode.DDA.getName()
        runParamTree.setCVParams(cvParams ++ Seq(
          CVParam(PsiMsCV.ACQUISITION_PARAMETER.getAccession(),"acquisition parameter",value=mode)
        ))
      }

      stmt.bind(3, _xmlSerializer.serializeParamTree(runParamTree))

      // FIXME: do not use default values
      stmt.bindNull(4)
      stmt.bind(5, 1)
      stmt.bind(6, 1)
      stmt.bind(7, 1)
      stmt.bind(8, 1)
      stmt.bind(9, 1)
      stmt.step()
      stmt.reset()
    }
    stmt.dispose()

    // --- INSERT SOURCE FILES --- //
    stmt = _connection.prepare(s"INSERT INTO ${SourceFileTable.tableName} VALUES (NULL, ?, ?, ?, NULL)", cached = false)
    val sourceFiles = metaData.getSourceFiles()
    for (sourceFile <- sourceFiles) {
      stmt.bind(1, sourceFile.getName)
      stmt.bind(2, sourceFile.getLocation)
      // FIXME: source file paramtree should be defined
      if (sourceFile.getParamTree().isEmpty) stmt.bind(3, "")
      else stmt.bind(3, _xmlSerializer.serializeParamTree(sourceFile.getParamTree().get))
      stmt.step()
      stmt.reset()
    }
    stmt.dispose()

    // --- INSERT SAMPLES --- //
    stmt = _connection.prepare(s"INSERT INTO ${SampleTable.tableName} VALUES (NULL, ?, ?, NULL)", cached = false)

    val samples = metaData.getSamples()
    if (samples.isEmpty) {
      val sampleName = sourceFiles.headOption.map(_.getName).getOrElse(dbLocation.getName.split('.').headOption.getOrElse("undefined"))
      stmt.bind(1, sampleName)
      stmt.step()
    } else {
      for (sample <- samples) {
        stmt.bind(1, sample.getName)
        if (sample.getParamTree().isEmpty) stmt.bindNull(2)
        else stmt.bind(2, _xmlSerializer.serializeParamTree(sample.getParamTree().get))
        stmt.step()
        stmt.reset()
      }
    }
    stmt.dispose()

    // --- INSERT SOFTWARE LIST --- //
    stmt = _connection.prepare(s"INSERT INTO ${SoftwareTable.tableName} VALUES (NULL, ?, ?, ?, NULL)", cached = false)
    val softwareList = metaData.getSoftwareList
    for (software <- softwareList) {
      stmt.bind(1, software.getName)
      stmt.bind(2, software.getVersion)
      // FIXME: software paramtree should be defined
      if (software.getParamTree().isEmpty) stmt.bind(3, "")
      else stmt.bind(3, _xmlSerializer.serializeParamTree(software.getParamTree().get))
      stmt.step()
      stmt.reset()
    }
    stmt.dispose()

    ()
  }

  // --- INSERT SPECTRUM DATA --- //
  def insertSpectrum(spectrum: Spectrum, metaDataAsText: SpectrumXmlMetaData, dataEncoding: DataEncoding): Unit = {

    val sh = spectrum.getHeader
    val sd = spectrum.getData
    val smd = metaDataAsText
    val peaksCount = sd.getPeaksCount()

    // FIXME: deal with empty spectra
    if (peaksCount == 0) return

    _insertedSpectraCount += 1

    val msLevel = sh.getMsLevel
    val isolationWindowOpt = if (isDIA && msLevel == 2) sh.isolationWindow else None // very important for cache
    //val spectrumId = sh.getId
    val spectrumId = _insertedSpectraCount // note: we maintain our own spectrum ID counter
    val spectrumTime = sh.getElutionTime()
    //_spectrumIdByTime(spectrumTime) = spectrumId
    //println("spectrumId is " + spectrumId)

    val dataEnc = this._dataEncodingRegistry.getOrAddDataEncoding(dataEncoding)
    val mzInc = (if (msLevel == 1) bbSizes.BB_MZ_HEIGHT_MS1 else bbSizes.BB_MZ_HEIGHT_MSn).toFloat

    // FIXME: how should we store empty spectra? should we create empty entries in existing BBs?
    var bbFirstSpectrumId = 0L
    if (peaksCount == 0) {
      val curBB = _getBBWithNextSpectrumSlice(spectrum,spectrumId,spectrumTime,msLevel,dataEnc,isolationWindowOpt)(0, 0, mzInc)
      bbFirstSpectrumId = curBB.spectrumIds.head
    } else {
      // FIXME: min m/z should be retrieve from meta-data (scan list)
      var curMinMz = math.round(sd.getMzAt(0) / bbSizes.BB_MZ_HEIGHT_MS1).toInt * bbSizes.BB_MZ_HEIGHT_MS1.toFloat

      //println(s"msLevel is $msLevel; min m/z is: $curMinMz")

      var curMaxMz = curMinMz + mzInc

      // FIXME: this is a workaround => find a better way to do this
      if (msLevel == 2 && !isDIA) {
        curMinMz = 0
        curMaxMz = bbSizes.BB_MZ_HEIGHT_MSn.toFloat
      }

      //println(s"msLevel is $msLevel; retained m/z range: $curMinMz/$curMaxMz")

      val isTimeForNewBBRow = _bbCache.isTimeForNewBBRow(msLevel, isolationWindowOpt, spectrumTime)

      // Flush BB row when we reach a new row (retention time exceeding size of the bounding box for this MS level)
      if (isTimeForNewBBRow) {
        //println("******************************************************* FLUSHING BB ROW ****************************************")
        _flushBBRow(msLevel, isolationWindowOpt)
      }

      // TODO: put _getBBWithNextSpectrumSlice back here when memory issues are fixed

      // Peaks lookup to create Bounding Boxes
      var i = 0
      var curBB: BoundingBox = null

      while (i < peaksCount) {
        val mz = sd.getMzAt(i)

        if (i == 0) {
          curBB = _getBBWithNextSpectrumSlice(spectrum,spectrumId,spectrumTime,msLevel,dataEnc,isolationWindowOpt)(i, curMinMz, curMaxMz)
          bbFirstSpectrumId = curBB.spectrumIds.head
        }
        else if (mz > curMaxMz) {
          // Creates new bounding boxes even for empty data => should be removed in mzDB V2
          while (mz > curMaxMz) {
            curMinMz += mzInc
            curMaxMz += mzInc

            // Very important: ensure run slices are created in increasing m/z order
            val runSliceBoundaries = (msLevel, curMinMz, curMaxMz)
            if (!_runSliceStructureFactory.hasRunSlice(runSliceBoundaries))
              _runSliceStructureFactory.addRunSlice(runSliceBoundaries)
          }

          curBB = _getBBWithNextSpectrumSlice(spectrum,spectrumId,spectrumTime,msLevel,dataEnc,isolationWindowOpt)(i, curMinMz, curMaxMz)
        }

        if (curBB.spectrumSlices.last.isDefined) {
          // Add data point to the Bounding Box
          val lastSpectrumSlice = curBB.spectrumSlices.last.get
          lastSpectrumSlice.lastPeakIdx = i
        }

        i += 1
      }
    }

    // --- INSERT SPECTRUM HEADER --- //
    val stmt = _spectrumInsertStmt
    stmt.bind(1, spectrumId)
    // FIXME: Proline should be able to work with files where the initialID differs from the mzDB ID, thus sh.getInitialId() should be used when it is fixed
    stmt.bind(2, spectrumId) // sh.getInitialId
    stmt.bind(3, sh.title)
    stmt.bind(4, sh.getCycle)
    stmt.bind(5, sh.getTime)
    stmt.bind(6, msLevel)

    if (sh.activationType.isEmpty) stmt.bindNull(7)
    else stmt.bind(7, sh.activationType.get.toString)

    stmt.bind(8, sh.getTic)
    stmt.bind(9, sh.getBasePeakMz)
    stmt.bind(10, sh.getBasePeakIntensity)

    // Precursor information
    if (sh.getPrecursorMz.isEmpty) stmt.bindNull(11)
    else stmt.bind(11, sh.getPrecursorMz.get)
    if (sh.getPrecursorCharge.isEmpty) stmt.bindNull(12)
    else stmt.bind(12, sh.getPrecursorCharge.get)

    stmt.bind(13, sh.getPeaksCount)

    // XML Meta-data bindings
    stmt.bind(14, smd.paramTree)
    stmt.bind(15, smd.scanList)

    if (smd.precursorList.isEmpty) stmt.bindNull(16)
    else {
      val precList = smd.precursorList.get
      stmt.bind(16, precList)
    }

    if (smd.productList.isEmpty) stmt.bindNull(17)
    else stmt.bind(17, smd.productList.get)

    stmt.bind(18, 1)
    stmt.bind(19, 1)
    stmt.bind(20, 1)
    stmt.bind(21, 1)
    stmt.bind(22, 1)
    stmt.bind(23, dataEnc.getId)
    stmt.bind(24, bbFirstSpectrumId)

    stmt.step()
    stmt.reset()

    ()
  } // ends insertSpectrum

  private def _getBBWithNextSpectrumSlice(
    spectrum: Spectrum, spectrumId: Long, spectrumTime: Float, msLevel: Int, dataEnc: DataEncoding, isolationWindowOpt: Option[IsolationWindow]
  )(peakIdx: Int, minMz: Float, maxMz: Float): BoundingBox = {

    val runSliceBoundaries = (msLevel, minMz, maxMz)
    val runSliceIdOpt = _runSliceStructureFactory.getRunSliceId(runSliceBoundaries)
    val runSliceId = if (runSliceIdOpt.isDefined) runSliceIdOpt.get
    else _runSliceStructureFactory.addRunSlice(runSliceBoundaries).id

    val cachedBBOpt = _bbCache.getCachedBoundingBox(runSliceId, isolationWindowOpt)

    val bb = if (cachedBBOpt.isEmpty) { // isTimeForNewBBRow ||
      // FIXME: perform this estimation by counting the number of run slices per MS level
      val sliceSlicesCountHint = if (msLevel == 2) 1
      else _runSliceStructureFactory.getRunSlicesCount()

      _bbCache.createBoundingBox(spectrumTime, runSliceId, msLevel, dataEnc, isolationWindowOpt, sliceSlicesCountHint)
    } else {
      val cachedBB = cachedBBOpt.get

      // Increase size of the bounding box for one new spectrum
      cachedBB.lastTime = spectrumTime // update BB last RT

      cachedBB
    }

    bb.spectrumIds += spectrumId
    //bb.spectrumSlices += Some(_bbCache.acquireSpectrumDataBuilder(sh, sh.peaksCount))
    bb.spectrumSlices += Some(SpectrumSliceIndex(spectrum.data,peakIdx,peakIdx))

    bb
  }

  private def _flushBBRow(msLevel: Int, isolationWindowOpt: Option[IsolationWindow]): Unit = {

    // Retrieve the longest list of spectra ids
    //val lcCtxBySpecId = new LongMap[ILcContext]()
    val spectraIds = new ArrayBuffer[Long]()
    _bbCache.forEachCachedBB(msLevel, isolationWindowOpt) { bb =>
      spectraIds ++= bb.spectrumIds
      /*bb.spectrumIds.zip(bb.spectrumSlices).foreach { case (id,sSliceOpt) =>
        val sSlice = sSliceOpt.get
        lcCtxBySpecId(id) = sSlice.spectrum.header
      }*/
    }

    //val distinctBBRowSpectraIds = lcCtxBySpecId.keys.toArray.sorted
    val distinctBBRowSpectraIds = spectraIds.distinct.sorted
    //println(distinctBBRowSpectraIds.toList)

    // Insert all BBs corresponding to the same MS level and the same isolation window (DIA only)
    _bbCache.forEachCachedBB(msLevel, isolationWindowOpt) { bb =>

      // Map slices by spectrum id
      val specSliceById = bb.spectrumIds.zip(bb.spectrumSlices).withFilter(_._2.isDefined).map( kv=> (kv._1,kv._2.get) ).toLongMap //With(ssOpt => ssOpt.get.spectrum.header.getSpectrumId -> ssOpt.get)

      // Create missing slices
      val spectraSlices = distinctBBRowSpectraIds.map { specId =>
        specSliceById.get(specId)
        //OrElse(_bbCache.acquireSpectrumDataBuilder(lcCtxBySpecId(specId),0))
      }

      // Update Bounding Box
      bb.spectrumIds.clear()
      bb.spectrumIds ++= distinctBBRowSpectraIds

      bb.spectrumSlices.clear()
      bb.spectrumSlices ++= spectraSlices

      // Insert Bounding Box
      this._insertAndIndexBoundingBox(bb)
    }

    // Remove BB row
    _bbCache.removeBBRow(msLevel, isolationWindowOpt)
  }

  // Method to be implemented in concrete implementations (JVM and Native ones for instance)
  protected def insertBoundingBox(bb: BoundingBox): Long

  private def _insertAndIndexBoundingBox(bb: BoundingBox): Unit = { // --- INSERT BOUNDING BOX --- //

    val msLevel = bb.msLevel
    val bbId = this.insertBoundingBox(bb)

    val runSlice = this._runSliceStructureFactory.getRunSlice(bb.runSliceId).get

    // TODO: insert this index at the end of the file creation
    var stmt: ISQLiteStatement = null
    val isRTreeIndexInserted: Boolean = if (msLevel == 1) {
      stmt = _rtreeInsertStmt

      stmt.bind(1, bbId)
      stmt.bind(2, runSlice.beginMz)
      stmt.bind(3, runSlice.endMz)
      stmt.bind(4, bb.firstTime)
      stmt.bind(5, bb.lastTime)

      true

    } else if (msLevel == 2 && isDIA) {

      // TODO: parse this in the MzMLParser?
      try { // TODO: remove this try/catch
        /*val precursorListStr = smd.precursorList.get
        val selectedIons = precursorListStr.split("selectedIon>")
        if (selectedIons.length <= 1) false
        else {
          var kv = Array.empty[String]
          val selectedIonMzOpt = selectedIons(1).split(" ").collectFirst { case s if { kv = s.split("="); kv(0) == "value" } => {
              val mzStr = kv(1)
              mzStr.substring(1, mzStr.length()-1).toDouble // remove quotes and convert to Double
            }
          }
          val selectedIonMz = selectedIonMzOpt.get

          val nearestWindowOpt = diaIsolationWindows.get.find { win: IsolationWindow =>
            win.minMz <= selectedIonMz && win.maxMz >= selectedIonMz
          }*/

        val isolationWindowOpt = bb.isolationWindow

        if (isolationWindowOpt.isEmpty) false
        else {
          stmt = _msnRtreeInsertStmt

          stmt.bind(1, bbId)
          stmt.bind(2, msLevel)
          stmt.bind(3, msLevel)
          stmt.bind(4, isolationWindowOpt.get.minMz)
          stmt.bind(5, isolationWindowOpt.get.maxMz)
          stmt.bind(6, runSlice.beginMz)
          stmt.bind(7, runSlice.endMz)
          stmt.bind(8, bb.firstTime)
          stmt.bind(9, bb.lastTime)

          true
        }

      } catch {
        case t: Throwable => {
          // TODO: use configured Logger
          println("Can't parse <selectedIon> XML String because: " + t.getMessage)
          false
        }
      }
    } else false

    if (isRTreeIndexInserted) {
      // execute R*Tree index insert statement
      stmt.step()
      stmt.reset()
    } else if (isDIA) {
      println(s"No R*Tree index inserted for BB with id = ${bb.id} ; MS level = $msLevel")
    }

    ()
  }

}

/*
case class SpectrumDataBuilder( // actually used to ceate a spectrum slice

  /** The LC context. */
  var lcContext: ILcContext,

  /** The mz list. */
  var mzList: Array[Double],

  /** The intensity list. */
  var intensityList: Array[Float],

  /** The left hwhm list. */
  var leftHwhmList: Array[Float], // TODO: optional/nullable?

  /** The right hwhm list. */
  var rightHwhmList: Array[Float] // TODO: optional/nullable?

) extends ISpectrumDataContainer {

  private var _peaksCount = 0

  def peaksCount: Int = _peaksCount

  // Implementation of ISpectrumData interface
  //def getDataEncoding(): IDataEncoding = dataEncoding

  def clear(): Unit = {
    _peaksCount = 0
    lcContext = null
  }

  def addPeak(mz: Double, intensity: Float): Unit = {
    mzList(_peaksCount) = mz
    intensityList(_peaksCount) += intensity
    _peaksCount += 1
    ()
  }

  def addPeak(mz: Double, intensity: Float, leftHwhm: Float, rightHwhm: Float): Unit = {
    if (leftHwhmList == null) leftHwhmList = new Array[Float](mzList.length)
    if (rightHwhmList == null) rightHwhmList = new Array[Float](mzList.length)

    mzList(_peaksCount) = mz
    intensityList(_peaksCount) = intensity
    leftHwhmList(_peaksCount) = leftHwhm
    rightHwhmList(_peaksCount) = rightHwhm

    _peaksCount += 1
    ()
  }

  def getMzAt(index: Int): Double = mzList(index)

  def getIntensityAt(index: Int): Float = intensityList(index)

  def getLeftHwhmAt(index: Int): Option[Float] = if (leftHwhmList != null) Some(leftHwhmList(index)) else None

  def getRightHwhmAt(index: Int): Option[Float] = if (rightHwhmList != null) Some(rightHwhmList(index)) else None

  def getPeaksCount(): Int = peaksCount

  def forEachPeak(lcContext: ILcContext)(fn: (IPeak,Int) => Unit): Unit = {

    var idx = 0

    object PeakCursor extends IPeak {
      def getMz(): Double = mzList(idx)
      def getIntensity(): Float = intensityList(idx)
      def getLeftHwhm(): Float = getLeftHwhmAt(idx).getOrElse(0f)
      def getRightHwhm(): Float = getRightHwhmAt(idx).getOrElse(0f)
      def getLcContext(): ILcContext = lcContext
    }

    while (idx < _peaksCount) {
      fn(PeakCursor,idx)
      idx += 1
    }
  }

  /**
    * Gets the min mz.
    *
    * @return the min mz
    */
  def getMinMz(): Double = { // supposed and i hope it will always be true that mzList is sorted
    // do not do any verification
    if (peaksCount == 0) return 0
    mzList(0)
  }

  /**
    * Gets the max mz.
    *
    * @return the max mz
    */
  def getMaxMz(): Double = {
    if (peaksCount == 0) return 0
    mzList(peaksCount - 1)
  }

  /**
    * Checks if is empty.
    *
    * @return true, if is empty
    */
  def isEmpty(): Boolean = { // supposing intensityList and others have the same size
    peaksCount == 0
  }

}*/

private[writer] case class SpectrumSliceIndex(
  spectrumData: ISpectrumData,
  var firstPeakIdx: Int,
  var lastPeakIdx: Int
) {
  def peaksCount(): Int = {
    assert(lastPeakIdx >=  firstPeakIdx, s"invalid pair of firstPeakIdx/lastPeakIdx (firstPeakIdx,$lastPeakIdx)")

    1 + lastPeakIdx - firstPeakIdx
  }
}

private[writer] case class BoundingBox(
  var id: Int = 0,
  var firstTime: Float = 0f,
  var lastTime: Float = 0f,
  var runSliceId: Int = 0,
  var msLevel: Int = 0,
  var dataEncoding: DataEncoding = null,
  var isolationWindow: Option[IsolationWindow] = None,
  var spectrumIds: ArrayBuffer[Long] = null,
  var spectrumSlices: ArrayBuffer[Option[SpectrumSliceIndex]] = null
)

private[writer] class BoundingBoxWriterCache(bbSizes: BBSizes) {

  private var bbId = 0

  private val boundingBoxMap = new HashMap[(Int,Option[IsolationWindow]), BoundingBox]
  // val boundingBoxMap = new collection.mutable.ListBuffer[((Int,Option[IsolationWindow]), BoundingBox)]
  //private val reusableBBs = new HashSet[BoundingBox] // TODO: remove this, not useful

  /*
  private val reusableSpecBuilders = new HashSet[SpectrumDataBuilder]
  private var sdbCount = 0

  def acquireSpectrumDataBuilder(lcContext: ILcContext, maxPeaksCount: Int): SpectrumDataBuilder = {
    //val sbOpt = reusableSpecBuilders.find(_.mzList.length >= maxPeaksCount)

    if (reusableSpecBuilders.isEmpty) {
      sdbCount += 1
      println(s"Acquired $sdbCount SpectrumDataBuilders")
      SpectrumDataBuilder(
        lcContext,
        new Array[Double](maxPeaksCount),
        new Array[Float](maxPeaksCount),
        null,
        null
      )
    }
    else {
      val sb = reusableSpecBuilders.head
      reusableSpecBuilders -= sb
      sb.lcContext = lcContext

      // Reset arrays ig length is not big enough
      if (sb.mzList.length < maxPeaksCount) {
        sb.mzList = new Array[Double](maxPeaksCount)
        sb.intensityList = new Array[Float](maxPeaksCount)
        if (sb.leftHwhmList != null || sb.rightHwhmList != null) {
          sb.leftHwhmList = new Array[Float](maxPeaksCount)
          sb.rightHwhmList = new Array[Float](maxPeaksCount)
        }
      }

      sb
    }
  }*/

  def isTimeForNewBBRow(msLevel: Int, isolationWindow: Option[IsolationWindow], curSpecTime: Float): Boolean = {
    val bbRowFirstSpecTimeOpt = _findBBFirstTime(msLevel, isolationWindow)
    if (bbRowFirstSpecTimeOpt.isEmpty) return true

    val maxRtWidth = if (msLevel == 1) bbSizes.BB_RT_WIDTH_MS1 else bbSizes.BB_RT_WIDTH_MSn
    if ( (curSpecTime - bbRowFirstSpecTimeOpt.get) > maxRtWidth) true else false
  }

  private def _findBBFirstTime(msLevel: Int, isolationWindow: Option[IsolationWindow]): Option[Float] = {
    this.forEachCachedBB(msLevel, isolationWindow) { bb =>
      return Some(bb.firstTime)
    }

    None
  }

  def forEachCachedBB(msLevel: Int, isolationWindow: Option[IsolationWindow])(boundingBoxFn: BoundingBox => Unit): Unit = {
    for (
      // FIXME: sorting by runSliceId is not sage => nothing ensures that run slice IDs are created in the right order
      ((runSliceId,isoWinOpt),bb) <- boundingBoxMap.toList.sortBy(_._2.runSliceId);
      if bb.msLevel == msLevel && isoWinOpt == isolationWindow
    ) {
      boundingBoxFn(bb)
    }
  }

  def removeBBRow(msLevel: Int, isolationWindow: Option[IsolationWindow]): Unit = {

    //val runSlicesToRemove = new ArrayBuffer[Int]()
    for (
      // TODO: do we need to sort?
      ((runSliceId,isoWinOpt),bb) <- boundingBoxMap.toList.sortBy(_._2.runSliceId);
      if bb.msLevel == msLevel && isoWinOpt == isolationWindow
    ) {
     /* runSlicesToRemove += runSliceId
    }

    runSlicesToRemove.foreach { runSliceId =>*/
      val removedBBOpt = boundingBoxMap.remove(runSliceId, isolationWindow)
      //val removedBBOpt = boundingBoxMap.find( _._1 == (runSliceId, isolationWindow) )

      if (removedBBOpt.isDefined) {
        //boundingBoxMap -= removedBBOpt.get

        /*
        for (sbOpt <- removedBBOpt.get.spectrumSlices; sb <- sbOpt) {
          sb.clear()
          reusableSpecBuilders += sb
        }
        */

        /*
        val bb = removedBBOpt.get._2
        bb.spectrumIds.clear()
        bb.spectrumSlices.clear()
        reusableBBs += bb // TODO: remove this reusableBBs feature
        */
      }
    }
  }

  def getBBRowsKeys(): List[(Int, Option[IsolationWindow])] = {

    val foundKeys = for (
      ((runSliceId,isoWinOpt),bb) <- boundingBoxMap.toList.sortBy(_._2.runSliceId)
    ) yield (bb.msLevel,isoWinOpt)

    foundKeys.distinct.sortBy { case (msLevel,isoWinOpt) =>
      (msLevel,isoWinOpt.map(_.minMz).getOrElse(0f))
    }
  }

  def getCachedBoundingBox(
    runSliceId: Int,
    isolationWindow: Option[IsolationWindow] // defined only for DIA data
  ): Option[BoundingBox] = {
    val bbKey = (runSliceId,isolationWindow)
    boundingBoxMap.find(_._1 == bbKey).map(_._2)
  }

  def createBoundingBox(
    spectrumTime: Float,
    runSliceId: Int,
    msLevel: Int,
    dataEncoding: DataEncoding,
    isolationWindow: Option[IsolationWindow], // defined only for DIA data,
    slicesCountHint: Int
  ): BoundingBox = {
    val bbKey = (runSliceId,isolationWindow)
    assert(! boundingBoxMap.contains(bbKey), "cannot create a new bounding box since cache has not been flushed")

    /*val newOrCachedBB = if (reusableBBs.isEmpty) {
      val newBB = BoundingBox()
      newBB.spectrumIds = new ArrayBuffer[Long](slicesCountHint)
      newBB.spectrumSlices = new ArrayBuffer[Option[SpectrumSliceIndex]](slicesCountHint)
      newBB
    } else {
      val bb = reusableBBs.head
      reusableBBs -= bb
      bb
    }*/

    val newOrCachedBB = BoundingBox(
      spectrumIds = new ArrayBuffer[Long](slicesCountHint),
      spectrumSlices = new ArrayBuffer[Option[SpectrumSliceIndex]](slicesCountHint),
      id = { bbId += 1; bbId },
      firstTime = spectrumTime,
      lastTime = spectrumTime,
      runSliceId = runSliceId,
      msLevel = msLevel,
      dataEncoding = dataEncoding,
      isolationWindow = isolationWindow
    )

    //boundingBoxMap ++= List( (bbKey, newOrCachedBB) )
    boundingBoxMap(bbKey) = newOrCachedBB
    //println(boundingBoxMap.size)

    newOrCachedBB
  }


}

// TODO: move to model package?
private[writer] class RunSliceStructureFactory(runId: Int) {
  private val runSlicesStructure = new HashMap[(Int,Float,Float), RunSliceHeader]
  private val runSliceById = new LongMap[RunSliceHeader]()

  private var runSliceId = 1

  def addRunSlice(msLevel: Int, beginMz: Float, endMz: Float): RunSliceHeader = {
    addRunSlice((msLevel, beginMz, endMz))
  }
  def addRunSlice(runSliceBoundaries: Tuple3[Int,Float,Float]): RunSliceHeader = {
    val runSlice = RunSliceHeader(runSliceId, runSliceBoundaries._1, -1, runSliceBoundaries._2, runSliceBoundaries._3, runId)
    runSlicesStructure(runSliceBoundaries) = runSlice
    runSliceById(runSlice.id) = runSlice
    runSliceId += 1
    runSlice
  }

  def hasRunSlice(msLevel: Int, beginMz: Float, endMz: Float): Boolean = {
    runSlicesStructure.contains((msLevel,beginMz,endMz))
  }
  def hasRunSlice(runSliceBoundaries: Tuple3[Int,Float,Float]): Boolean = {
    runSlicesStructure.contains(runSliceBoundaries)
  }

  def getRunSliceId(msLevel: Int, beginMz: Float, endMz: Float): Option[Int] = {
    runSlicesStructure.get((msLevel, beginMz, endMz)).map(_.id)
  }
  def getRunSliceId(runSliceBoundaries: Tuple3[Int,Float,Float]): Option[Int] = {
    runSlicesStructure.get(runSliceBoundaries).map(_.id)
  }

  def getRunSlice(id: Int): Option[RunSliceHeader] = {
    runSliceById.get(id)
  }

  def getAllRunSlices(): Seq[RunSliceHeader] = {
    var runSliceNumber = 0
    runSlicesStructure.values.groupBy(_.msLevel).toSeq.sortBy(_._1).flatMap { case (msLevel,msLevelRunSlices) =>
      msLevelRunSlices.toList.sortBy(_.id).map { runSlice =>
        runSliceNumber += 1
        runSlice.copy(number = runSliceNumber)
      }
    }
  }

  def getRunSlicesCount(): Int = runSliceById.size
}

private[writer] class DataEncodingRegistry() {
  private val dataEncodingMap = new HashMap[(DataMode.Value, PeakEncoding.Value), DataEncoding]

  private var dataEncodingId = 0

  def getOrAddDataEncoding(dataEnc: IDataEncoding): DataEncoding = {
    dataEncodingMap.getOrElseUpdate(
      (dataEnc.getMode,dataEnc.getPeakEncoding),
      DataEncoding(
        id = { dataEncodingId += 1; dataEncodingId },
        mode = dataEnc.getMode,
        peakEncoding = dataEnc.getPeakEncoding(),
        compression = dataEnc.getCompression(),
        byteOrder = dataEnc.getByteOrder()
      )
    )
  }

  def getDistinctDataEncodings(): Seq[DataEncoding] = {
    dataEncodingMap.values.toSeq.sortBy(_.id)
  }

  /*def getOrAddDataEncoding(dataMode: DataMode.Value, peakEncoding: PeakEncoding.Value): DataEncoding = {
    this.getOrAddDataEncoding(
      DataEncoding(id = 1, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = ByteOrder.LITTLE_ENDIAN)
    )
  }*/

}
