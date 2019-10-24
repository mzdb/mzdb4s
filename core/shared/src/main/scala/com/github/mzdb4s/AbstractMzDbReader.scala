package com.github.mzdb4s

import java.io.File

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params.param._
import com.github.mzdb4s.db.table.{BoundingBoxTable, SpectrumTable}
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.MzDbReaderQueries
import com.github.mzdb4s.io.reader.bb.BoundingBoxBuilder
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder.SpectrumDataBuilder
import com.github.mzdb4s.util.ms.MsUtils

object AbstractMzDbReader {

  // Important: SQlite R*Tree floating values are 32bits floats, thus we need to expand the search slightly
  // Advice from SQLite developers (https://www.sqlite.org/rtree.html#roundoff_error):
  // Applications should expand their contained-within query boxes slightly (by 0.000012%)
  // by rounding down the lower coordinates and rounding up the top coordinates, in each dimension.
  private val SQLITE_RTREE_UB_CORR = 1.0 + 0.00000012
  private val SQLITE_RTREE_LB_CORR = 1.0 - 0.00000012

}

/**
  * Allows to manipulates data contained in the mzDB file.
  *
  * @author David
  */
abstract class AbstractMzDbReader {

  /** Some fields initialized in the constructor **/
  def dbFile: File
  def entityCache: Option[MzDbEntityCache]
  protected def mzDbHeader: MzDbHeader
  protected def paramNameGetter: IMzDbParamName

  protected lazy val bbSizes: BBSizes = BBSizes(
    BB_MZ_HEIGHT_MS1 = this.mzDbHeader.getUserParam(paramNameGetter.ms1BBMzWidthParamName).getValue.toDouble,
    BB_MZ_HEIGHT_MSn = this.mzDbHeader.getUserParam(paramNameGetter.msnBBMzWidthParamName).getValue.toDouble,
    BB_RT_WIDTH_MS1 = this.mzDbHeader.getUserParam(paramNameGetter.ms1BBTimeWidthParamName).getValue.toFloat,
    BB_RT_WIDTH_MSn = this.mzDbHeader.getUserParam(paramNameGetter.ms1BBTimeWidthParamName).getValue.toFloat
  )

  /**
    * The is no loss mode. If no loss mode is enabled, all data points will be encoded as highres, i.e. 64 bits mz and 64 bits int. No peak picking and not
    * fitting will be performed on profile data.
    */
  lazy val isNoLossMode: Boolean = {
    if (this.getMzDbHeader().getUserParam(this.paramNameGetter.lossStateParamName).getValue == "false") false else true
  }

  /** Some fields related to XML data loading **/
  private var _loadParamTree = false
  private var _loadScanList = false
  private var _loadPrecursorList = false

  /** Define some lazy fields **/
  // TODO: find a CV param representing the information better
  protected var acquisitionMode: AcquisitionMode.Value = _
  protected var diaIsolationWindows: Seq[IsolationWindow] = _
  protected var instrumentConfigs: Seq[InstrumentConfiguration] = _
  protected var runs: Seq[Run] = _
  protected var samples: Seq[Sample] = _
  protected var softwareList: Seq[Software] = _
  protected var sourceFiles: Seq[SourceFile] = _

  /**
    * Close the file to avoid memory leaks. Method to be implemented in child classes.
    */
  def close(): Unit

  def getDataEncodingReader(): AbstractDataEncodingReader

  def getSpectrumHeaderReader(): AbstractSpectrumHeaderReader

  def getRunSliceHeaderReader(): AbstractRunSliceHeaderReader

  def isParamTreeLoadingEnabled: Boolean = _loadParamTree

  def enableParamTreeLoading(): Unit = _loadParamTree = true

  def isScanListLoadingEnabled: Boolean = _loadScanList

  def enableScanListLoading(): Unit = _loadScanList = true

  def isPrecursorListLoadingEnabled: Boolean = _loadPrecursorList

  def enablePrecursorListLoading(): Unit = _loadPrecursorList = true

  def getEntityCache(): Option[MzDbEntityCache] = this.entityCache

  def getDbLocation(): String = this.dbFile.getAbsolutePath

  def getMzDbHeader(): MzDbHeader = this.mzDbHeader

  def getBBSizes(): BBSizes = this.bbSizes

  protected def createMetaData(dataEncodings: Seq[DataEncoding]): MzDbMetaData = {
    MzDbMetaData(
      mzDbHeader = this.mzDbHeader,
      dataEncodings = dataEncodings,
      commonInstrumentParams = CommonInstrumentParams(1, params.ParamTree()), // FIXME: fill me
      instrumentConfigurations = this.getInstrumentConfigurations(),
      processingMethods = Seq.empty[ProcessingMethod], // FIXME: fill me
      runs = this.getRuns(),
      samples = this.getSamples(),
      softwareList = this.getSoftwareList(),
      sourceFiles = this.getSourceFiles()
    )
  }

  protected def getSpectraXmlMetaData()(implicit mzDbCtx: MzDbContext): Seq[SpectrumXmlMetaData] = {
    val spectraCount = MzDbReaderQueries.getSpectraCount()

    val sqlString = "SELECT id, param_tree, scan_list, precursor_list, product_list FROM spectrum"
    val records = mzDbCtx.newSQLiteQuery(sqlString)getRecordIterator()

    val xmlMetaDataBuffer = new ArrayBuffer[SpectrumXmlMetaData](spectraCount)
    while (records.hasNext) {
      val r = records.next

      xmlMetaDataBuffer += SpectrumXmlMetaData(
        spectrumId = r.columnLong(SpectrumTable.ID),
        paramTree = r.columnString(SpectrumTable.PARAM_TREE),
        scanList = r.columnString(SpectrumTable.SCAN_LIST),
        precursorList = Option(r.columnString(SpectrumTable.PRECURSOR_LIST)),
        productList = Option(r.columnString(SpectrumTable.PRODUCT_LIST))
      )
    }

    xmlMetaDataBuffer
  }

  protected def getRunSliceData(runSliceId: Int)(implicit mzDbCtx: MzDbContext): RunSliceData = {

    // Rretrieve the number of BBs to read
    var queryStr = "SELECT count(id) FROM bounding_box WHERE run_slice_id = ? ORDER BY first_spectrum_id"
    val bbCount = mzDbCtx.newSQLiteQuery(queryStr).bind(1, runSliceId).extractSingleInt()

    // TODO: DBO => why a JOIN here ???
    // String queryStr = "SELECT bounding_box.* FROM bounding_box, bounding_box_rtree"
    // + " WHERE bounding_box.id = bounding_box_rtree.id AND bounding_box.run_slice_id = ?"
    // + " ORDER BY first_spectrum_id"; // number
    queryStr = "SELECT * FROM bounding_box WHERE run_slice_id = ? ORDER BY first_spectrum_id" // number
    // SQLiteStatement stmt =
    // connection.prepare("SELECT * FROM run_slice WHERE ms_level="+msLevel+" ORDER BY begin_mz ",
    // false);//number ASC", false);

    val records = mzDbCtx.newSQLiteQuery(queryStr).bind(1, runSliceId).getRecordIterator()
    val bbs = new ArrayBuffer[BoundingBox](bbCount)

    // FIXME: getSpectrumHeaderById
    val spectrumHeaderById = this.getSpectrumHeaderReader().getMs1SpectrumHeaderById()
    val dataEncodingBySpectrumId = this.getDataEncodingReader().getDataEncodingBySpectrumId()

    while (records.hasNext) {
      val record = records.next
      val bbId = record.columnInt(BoundingBoxTable.ID)
      val data = record.columnBlob(BoundingBoxTable.DATA)
      val firstSpectrumId = record.columnInt(BoundingBoxTable.FIRST_SPECTRUM_ID)
      val lastSpectrumId = record.columnInt(BoundingBoxTable.LAST_SPECTRUM_ID)
      // float minTime = (float) stmt.columnDouble(3);

      val bb = BoundingBoxBuilder.buildBB(bbId, data, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
      bb.setRunSliceId(runSliceId)

      bbs += bb
    }

    // TODO: check if faster than order by
    // Collections.sort(bbs); //sort bbs by their rt_min

    val spectrumSlices = bbs.flatMap(_.toSpectrumSlices()).toArray

    new RunSliceData(runSliceId, spectrumSlices)
  }

  protected def getSpectrumData(spectrumId: Long)(implicit mzDbCtx: MzDbContext): ISpectrumData = {
    val spectrumHeaderById = this.getSpectrumHeaderReader().getSpectrumHeaderById()
    val dataEncodingBySpectrumId = this.getDataEncodingReader().getDataEncodingBySpectrumId()
    val firstSpectrumId = spectrumHeaderById(spectrumId).bbFirstSpectrumId

    val sqlString = "SELECT * FROM bounding_box WHERE bounding_box.first_spectrum_id = ?"
    val records = mzDbCtx.newSQLiteQuery(sqlString).bind(1, firstSpectrumId).getRecordIterator()

    val bbS = new ArrayBuffer[BoundingBox]
    while (records.hasNext) {
      val r = records.next
      val lastSpectrumId = r.columnInt(BoundingBoxTable.LAST_SPECTRUM_ID)
      val bb = BoundingBoxBuilder.buildBB(r.columnInt(BoundingBoxTable.ID), r.columnBlob(BoundingBoxTable.DATA), firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
      bb.setRunSliceId(r.columnInt(BoundingBoxTable.RUN_SLICE_ID))
      bbS += bb
    }

    //val totalSpectraCount = bbS.foldLeft(0.0){(s , bb) => s + bb.getSpectraCount()}

    // Construct empty spectrum data
    val sdList = new ArrayBuffer[ISpectrumData](bbS.length)
    var peaksCount = 0

    for (bb <- bbS) {
      val bbReader = bb.reader
      // Retrieve only slices corresponding to the provided spectrum id
      val nbSpectra = bb.getSpectraCount()

      var spectrumData: ISpectrumData = null
      var spectrumIdx = 0
      while (spectrumIdx < nbSpectra && spectrumData == null) {
        if (spectrumId == bbReader.getSpectrumIdAt(spectrumIdx)) {
          spectrumData = bbReader.readSpectrumSliceDataAt(spectrumIdx)
          peaksCount += spectrumData.peaksCount
        }
        spectrumIdx += 1
      }

      sdList += spectrumData
      //sdBuilder.addSpectrumData(spectrumData)
    }

    SpectrumDataBuilder.mergeSpectrumDataList(sdList, peaksCount)
  }

  protected def getSpectrum(spectrumId: Long)(implicit mzDbCtx: MzDbContext): Spectrum = {
    val sh = this.getSpectrumHeaderReader().getSpectrumHeader(spectrumId)
    val sd = this.getSpectrumData(spectrumId)
    Spectrum(sh, sd)
  }

  protected def getSpectrumPeaks(spectrumId: Int)(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    this.getSpectrum(spectrumId).toPeaks()
  }

  protected def getMsSpectrumSlices(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float)(implicit mzDbCtx: MzDbContext): Array[SpectrumSlice] = {
    this._getSpectrumSlicesInRanges(minMz, maxMz, minRt, maxRt, 1, 0.0)
  }

  // TODO: think about msLevel > 2
  protected def getMsnSpectrumSlices(
    parentMz: Double,
    minFragMz: Double,
    maxFragMz: Double,
    minRt: Float,
    maxRt: Float
  )(implicit mzDbCtx: MzDbContext): Array[SpectrumSlice] = {
    this._getSpectrumSlicesInRanges(minFragMz, maxFragMz, minRt, maxRt, 2, parentMz)
    //return this._getSpectrumSlicesInRangesUsingMultiThreading(minFragMz, maxFragMz, minRt, maxRt, 2, parentMz);
  }

  private def _getSpectrumSlicesInRanges(
    minMz: Double,
    maxMz: Double,
    minRt: Float,
    maxRt: Float,
    msLevel: Int,
    parentMz: Double
  )(implicit mzDbCtx: MzDbContext): Array[SpectrumSlice] = {
    val sizes = this.getBBSizes()
    val rtWidth = if (msLevel == 1) sizes.BB_RT_WIDTH_MS1 else sizes.BB_RT_WIDTH_MSn
    val mzHeight = if (msLevel == 1) sizes.BB_MZ_HEIGHT_MS1 else sizes.BB_MZ_HEIGHT_MSn

    val _minMz = (minMz - mzHeight) * AbstractMzDbReader.SQLITE_RTREE_LB_CORR
    val _maxMz = (maxMz + mzHeight) * AbstractMzDbReader.SQLITE_RTREE_UB_CORR
    val _minRt = minRt - rtWidth
    val _maxRt = maxRt + rtWidth

    //System.out.println("Boundaries: " + _minMz + " "+ _maxMz + " "+ _minRt + " "+ _maxRt + " ");
    // TODO: query using bounding_box_msn_rtree to use the min_ms_level information even for MS1 data ???
    val sqliteQuery = if (msLevel == 1) {
      val sqlQuery = "SELECT * FROM bounding_box WHERE id IN " +
        "(SELECT id FROM bounding_box_rtree WHERE min_mz >= ? AND max_mz <= ? AND min_time >= ? AND max_time <= ? )" + //+ "(SELECT id FROM bounding_box_rtree WHERE min_mz >= 0.0 AND max_mz <= "+_maxMz +")"
        " ORDER BY first_spectrum_id"
      mzDbCtx.newSQLiteQuery(sqlQuery, false)
        .bind(1, _minMz)
        .bind(2, _maxMz)
        .bind(3, _minRt)
        .bind(4, _maxRt)
    }
    else {
      val sqlQuery = "SELECT * FROM bounding_box WHERE id IN " + "(SELECT id FROM bounding_box_msn_rtree" +
        " WHERE min_ms_level = " + msLevel + " AND max_ms_level = " + msLevel + " AND min_parent_mz <= ? AND max_parent_mz >= ? " +
        " AND min_mz >= ? AND max_mz <= ? AND min_time >= ? AND max_time <= ? )" + " ORDER BY first_spectrum_id"
      mzDbCtx.newSQLiteQuery(sqlQuery, false)
        .bind(1, parentMz)
        .bind(2, parentMz)
        .bind(3, _minMz)
        .bind(4, _maxMz)
        .bind(5, _minRt)
        .bind(6, _maxRt)
    }

    val recordIter = sqliteQuery.getRecordIterator()

    var spectrumHeaderById: mutable.LongMap[SpectrumHeader] = null
    if (msLevel == 1) spectrumHeaderById = this.getSpectrumHeaderReader().getMs1SpectrumHeaderById()
    else if (msLevel == 2) spectrumHeaderById = this.getSpectrumHeaderReader().getMs2SpectrumHeaderById()
    else throw new IllegalArgumentException("unsupported MS level: " + msLevel)

    val dataEncodingBySpectrumId = this.getDataEncodingReader().getDataEncodingBySpectrumId()
    val spectrumDataListById = new mutable.LongMap[ArrayBuffer[ISpectrumData]](100)
    val peaksCountBySpectrumId = new mutable.LongMap[Integer](100)

    // Iterate over bounding boxes
    while (recordIter.hasNext) { //System.out.println(".");
      val record = recordIter.next
      val bbId = record.columnInt(BoundingBoxTable.ID)

      // Retrieve bounding box data
      val data = record.columnBlob(BoundingBoxTable.DATA)
      val firstSpectrumId = record.columnLong(BoundingBoxTable.FIRST_SPECTRUM_ID)
      val lastSpectrumId = record.columnLong(BoundingBoxTable.LAST_SPECTRUM_ID)

      // Build the Bounding Box
      val bb = BoundingBoxBuilder.buildBB(bbId, data, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
      // bb.setRunSliceId(record.columnInt(BoundingBoxTable.RUN_SLICE_ID));

      val bbReader = bb.getReader
      val bbSpectraCount = bbReader.getSpectraCount
      val bbSpectrumIds = bbReader.getAllSpectrumIds

      // Iterate over each spectrum
      var spectrumIdx = 0
      while (spectrumIdx < bbSpectraCount) {
        val spectrumId = bbSpectrumIds(spectrumIdx)
        val sh = spectrumHeaderById(spectrumId)
        val currentRt = sh.getElutionTime()

        // Filtering on time dimension
        if (currentRt >= minRt && currentRt <= maxRt) {
          // Filtering on m/z dimension
          val spectrumSliceData = bbReader.readFilteredSpectrumSliceDataAt(spectrumIdx, minMz, maxMz)

          if (! spectrumSliceData.isEmpty()) {
            if (! spectrumDataListById.contains(spectrumId)) {
              spectrumDataListById.put(spectrumId, new ArrayBuffer[ISpectrumData])
              peaksCountBySpectrumId.put(spectrumId, 0)
            }

            spectrumDataListById(spectrumId) += spectrumSliceData
            peaksCountBySpectrumId.put(spectrumId, peaksCountBySpectrumId(spectrumId) + spectrumSliceData.peaksCount)
          }
        }

        spectrumIdx += 1
      }
    }

    val finalSpectrumSlices = new Array[SpectrumSlice](spectrumDataListById.size)
    var spectrumIdx = 0
    for ( (spectrumId,spectrumDataList) <- spectrumDataListById.toSeq.sortBy(_._1)) {
      val peaksCount = peaksCountBySpectrumId(spectrumId)
      val finalSpectrumData = SpectrumDataBuilder.mergeSpectrumDataList(spectrumDataList, peaksCount)

      finalSpectrumSlices(spectrumIdx) = SpectrumSlice(spectrumHeaderById(spectrumId), finalSpectrumData)

      spectrumIdx += 1
    }

    finalSpectrumSlices
  }

  /*
  private def _getSpectrumSlicesInRangesUsingMultiThreading(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float, msLevel: Int, parentMz: Double) = {
    val sizes = this.getBBSizes()
    val rtWidth = if (msLevel == 1) sizes.BB_RT_WIDTH_MS1 else sizes.BB_RT_WIDTH_MSn
    val mzHeight = if (msLevel == 1) sizes.BB_MZ_HEIGHT_MS1 else sizes.BB_MZ_HEIGHT_MSn

    val _minMz = minMz - mzHeight
    val _maxMz = maxMz + mzHeight
    val _minRt = minRt - rtWidth
    val _maxRt = maxRt + rtWidth

    var sqliteQuery = null
    if (msLevel == 1) {
      val sqlQuery = "SELECT * FROM bounding_box WHERE id IN " + "(SELECT id FROM bounding_box_rtree WHERE min_mz >= ? AND max_mz <= ? AND min_time >= ? AND max_time <= ? )" + " ORDER BY first_spectrum_id"
      sqliteQuery = new Nothing(connection, sqlQuery, false).bind(1, _minMz).bind(2, _maxMz).bind(3, _minRt).bind(4, _maxRt)
    }
    else {
      val sqlQuery = "SELECT * FROM bounding_box WHERE id IN " + "(SELECT id FROM bounding_box_msn_rtree" + " WHERE min_ms_level = " + msLevel + " AND max_ms_level = " + msLevel + " AND min_parent_mz <= ? AND max_parent_mz >= ? " + " AND min_mz >= ? AND max_mz <= ? AND min_time >= ? AND max_time <= ? )" + " ORDER BY first_spectrum_id"
      sqliteQuery = new Nothing(connection, sqlQuery, false).bind(1, parentMz).bind(2, parentMz).bind(3, _minMz).bind(4, _maxMz).bind(5, _minRt).bind(6, _maxRt)
    }

    val recordIter = sqliteQuery.getRecordIterator
    var tmpSpectrumHeaderById = null
    if (msLevel == 1) tmpSpectrumHeaderById = this.getSpectrumHeaderReader.getMs1SpectrumHeaderById(connection)
    else if (msLevel == 2) tmpSpectrumHeaderById = this.getSpectrumHeaderReader.getMs2SpectrumHeaderById(connection)
    else throw new IllegalArgumentException("unsupported MS level: " + msLevel)
    val spectrumHeaderById = tmpSpectrumHeaderById
    val dataEncodingBySpectrumId = this.getDataEncodingReader.getDataEncodingBySpectrumId(connection)
    // Create a SpectrumData Observable that will help us to process them in parallel
    val spectrumDataObservable = Observable.create((spectrumDataSubscriber) => {
      def foo(spectrumDataSubscriber) = {
        val bbObservable = Observable.create((bBoxSubscriber) => {
          def foo(bBoxSubscriber) = {
            try while ( {recordIter.hasNext}) {
              val record = recordIter.next
              val bbId = record.columnInt(BoundingBoxTable.ID)
              val data = record.columnBlob(BoundingBoxTable.DATA)
              val firstSpectrumId = record.columnLong(BoundingBoxTable.FIRST_SPECTRUM_ID)
              val lastSpectrumId = record.columnLong(BoundingBoxTable.LAST_SPECTRUM_ID)
              val bb = BoundingBoxBuilder.buildBB(bbId, data, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
              //System.out.println(Thread.currentThread().getName() + ": " +bb.getId() + " #spectra="+ bb.getSpectraCount());
              bBoxSubscriber.onNext(new Nothing(bb, !recordIter.hasNext))
            }
            catch {
              case e: Exception =>
                //bBoxSubscriber.onError(e);
                spectrumDataSubscriber.onError(e)
            }
            if (!bBoxSubscriber.isUnsubscribed) bBoxSubscriber.onCompleted
          }

          foo(bBoxSubscriber)
        }
        )
        // TODO: schedule the observer to process BBs in parallel threads
        val onEachBBox = new Nothing() {
          def call(pair: Nothing): Unit = {
            val bb = pair.left
            val isLastBB = pair.right
            // begins BB parsing
            // should be done in parallel
            val bbReader = bb.getReader
            val bbSpectraCount = bbReader.getSpectraCount
            val bbSpectrumIds = bbReader.getAllSpectrumIds
            // Iterate over each spectrum of the bounding box
            var spectrumIdx = 0
            while ( {spectrumIdx < bbSpectraCount}) {
              val spectrumId = bbSpectrumIds(spectrumIdx)
              val sh = spectrumHeaderById.get(spectrumId)
              val currentRt = sh.getElutionTime
              if ((currentRt >= minRt) && (currentRt <= maxRt)) {
                val spectrumSliceData = bbReader.readFilteredSpectrumSliceDataAt(spectrumIdx, minMz, maxMz)
                if (spectrumSliceData.isEmpty eq false) { //System.out.println(Thread.currentThread().getName() + ", subscriber: " +spectrumSliceData.getMinMz() + " #peaks="+ spectrumSliceData.getPeaksCount());
                  // Emit the spectrumSliceData and corresponding spectrum id in the observed stream
                  val p = new Nothing(spectrumId, spectrumSliceData)
                  spectrumDataSubscriber.onNext(p)
                }
              }
              {spectrumIdx += 1; spectrumIdx - 1}
            }
            if (isLastBB) spectrumDataSubscriber.onCompleted
            // ends BB parsing
          }
        }
        bbObservable.observeOn(AbstractMzDbReader.rxCompScheduler).subscribe(onEachBBox, (error) => System.out.println("Error during BBs reading" + error.getMessage), () => {
          def foo() = {
          }

          foo()
        } //System.out.println("Got all BBs!"))
      }

      foo(spectrumDataSubscriber)
    }
    )
    val spectrumDataListById = new util.TreeMap[Long, util.ArrayList[Nothing]]
    val peaksCountBySpectrumId = new util.HashMap[Long, Integer]
    val onEachSpectrumData = new Nothing() {
      def call(pair: Nothing): Unit = {
        val spectrumId = pair.left
        val spectrumSliceData = pair.right
        //System.out.println(Thread.currentThread().getName() + ", observer: " +spectrumSliceData.getMinMz() + " #peaks="+ spectrumSliceData.getPeaksCount());
        if (spectrumDataListById.containsKey(spectrumId) == false) {
          spectrumDataListById.put(spectrumId, new util.ArrayList[Nothing])
          peaksCountBySpectrumId.put(spectrumId, 0)
        }
        spectrumDataListById.get(spectrumId).add(spectrumSliceData)
        peaksCountBySpectrumId.put(spectrumId, peaksCountBySpectrumId.get(spectrumId) + spectrumSliceData.getPeaksCount)
      }
    }
    spectrumDataObservable.toBlocking.subscribe(onEachSpectrumData, (error) => System.out.println("Error during slices reading" + error.getMessage), () => {
      def foo() = {
      }

      foo()
    } //System.out.println("Got all slices!"))
    // TODO: create a method that returns the spectrumSliceData stream without post-processing ?
    //System.out.println(Thread.currentThread().getName() + ", spectrumDataListById.size: "+ spectrumDataListById.size());
    // Post-process the HashMap spectrumDataListById to produce a final array of spectrum slices
    val finalSpectrumSlices = new Array[Nothing](spectrumDataListById.size)
    var spectrumIdx = 0
    import scala.collection.JavaConversions._
    for (entry <- spectrumDataListById.entrySet) {
      val spectrumId = entry.getKey
      val spectrumDataList = entry.getValue
      val peaksCount = peaksCountBySpectrumId.get(spectrumId)
      val finalSpectrumData = _mergeSpectrumDataList(spectrumDataList, peaksCount)
      finalSpectrumSlices(spectrumIdx) = new Nothing(spectrumHeaderById.get(spectrumId), finalSpectrumData)
      spectrumIdx += 1
    }
    finalSpectrumSlices
  }*/

  /**
    * Lazy loading of the acquisition mode, parameter
    *
    */
  protected def getAcquisitionMode()(implicit mzDbCtx: MzDbContext): AcquisitionMode.Value = {
    if (this.acquisitionMode == null) {
      /*
      * final String sqlString = "SELECT param_tree FROM run"; final String runParamTree = new
      * SQLiteQuery(connection, sqlString).extractSingleString(); final ParamTree runTree =
      * ParamTreeParser.parseParamTree(runParamTree);
      */
      val runs = this.getRuns()
      val run0 = runs.head

      val runTree = run0.getOrLoadParamTree()
      try {
        val cvParam = runTree.getCVParam(PsiMsCV.ACQUISITION_PARAMETER)
        if (cvParam == null) return AcquisitionMode.UNKNOWN

        val value = cvParam.getValue()
        this.acquisitionMode = AcquisitionMode.getAcquisitionMode(value)
      } catch {
        case e: Exception =>
          this.acquisitionMode = AcquisitionMode.UNKNOWN
      }
    }

    this.acquisitionMode
  }

  protected def getDIAIsolationWindows()(implicit mzDbCtx: MzDbContext): Seq[IsolationWindow] = { // If DIA acquisition, the list will be computed on first use (lazy loading)
    // Will be always null on non DIA acquisition
    if (this.diaIsolationWindows == null) { // FIXME: in version 0.9.8 bounding_box_msn_rtree table should be empty.

      val sqlQuery = "SELECT DISTINCT min_parent_mz, max_parent_mz FROM bounding_box_msn_rtree ORDER BY min_parent_mz"
      val recordIt = mzDbCtx.newSQLiteQuery(sqlQuery).getRecordIterator()

      val isolationWindowList = new ArrayBuffer[IsolationWindow]
      while (recordIt.hasNext) {
        val record = recordIt.next
        val minMz = record.columnDouble("min_parent_mz")
        val maxMz = record.columnDouble("max_parent_mz")
        isolationWindowList += IsolationWindow(minMz.toFloat, maxMz.toFloat, ((minMz + maxMz) / 2).toFloat)
      }

      this.diaIsolationWindows = isolationWindowList
    }
    this.diaIsolationWindows
  }

  def getInstrumentConfigurations(): Seq[InstrumentConfiguration]

  def getRuns(): Seq[Run]

  def getSamples(): Seq[Sample]

  def getSoftwareList(): Seq[Software]

  def getSourceFiles(): Seq[SourceFile]

  def getFirstSourceFileName(): String = {
    this.getSourceFiles.head.getName
    // String sqlString = "SELECT name FROM source_file LIMIT 1";
    // return new SQLiteQuery(connection, sqlString).extractSingleString();
  }

  protected def getMsXicInMzRange(
    minMz: Double,
    maxMz: Double,
    method: XicMethod.Value
  )(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    this.getMsXic(minMz, maxMz, -1, -1, method)
  }

  protected def getMsXicInMzRtRanges(
    minMz: Double,
    maxMz: Double,
    minRt: Float,
    maxRt: Float,
    method: XicMethod.Value
  )(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    val mzCenter = (minMz + maxMz) / 2
    val mzTolInDa = maxMz - mzCenter
    this.getMsXic(mzCenter, mzTolInDa, minRt, maxRt, method)
  }

  protected def getMsXic(
    mz: Double,
    mzTolInDa: Double,
    minRt: Float,
    maxRt: Float,
    method: XicMethod.Value
  )(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    val minMz = mz - mzTolInDa
    val maxMz = mz + mzTolInDa
    val minRtForRtree = if (minRt >= 0) minRt else 0
    val maxRtForRtree = if (maxRt > 0) maxRt else MzDbReaderQueries.getLastTime()

    val spectrumSlices = this.getMsSpectrumSlices(minMz, maxMz, minRtForRtree, maxRtForRtree)

    val mzTolPPM = MsUtils.DaToPPM(mz, mzTolInDa)
    this._spectrumSlicesToXIC(spectrumSlices, mz, mzTolPPM, method)
  }

  protected def getMsnXic(
    parentMz: Double,
    fragmentMz: Double,
    fragmentMzTolInDa: Double,
    minRt: Float,
    maxRt: Float,
    method: XicMethod.Value
  )(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    val minFragMz = fragmentMz - fragmentMzTolInDa
    val maxFragMz = fragmentMz + fragmentMzTolInDa
    val minRtForRtree = if (minRt >= 0) minRt else 0
    val maxRtForRtree = if (maxRt > 0) maxRt else MzDbReaderQueries.getLastTime()

    val spectrumSlices = this.getMsnSpectrumSlices(parentMz, minFragMz, maxFragMz, minRtForRtree, maxRtForRtree)

    val fragMzTolPPM = MsUtils.DaToPPM(fragmentMz, fragmentMzTolInDa)
    this._spectrumSlicesToXIC(spectrumSlices, fragmentMz, fragMzTolPPM, method)
  }

  private def _spectrumSlicesToXIC(
    spectrumSlices: Array[SpectrumSlice],
    searchedMz: Double,
    mzTolPPM: Double,
    method: XicMethod.Value
  ): Array[IPeak] = {
    require(spectrumSlices != null, "spectrumSlices is null")

    if (spectrumSlices.length == 0) { // logger.warn("Empty spectrumSlices, too narrow request ?");
      return Array.empty[IPeak]
    }

    val spectrumSlicesCount = spectrumSlices.length
    val xicPeaks = new ArrayBuffer[IPeak](spectrumSlicesCount)

    method match {
      case XicMethod.MAX =>
        var i = 0
        while (i < spectrumSlicesCount) {
          val sl = spectrumSlices(i)
          val peaks = sl.toPeaks()
          val peaksCount = peaks.length

          if (peaksCount != 0) {
            //java.util.Arrays.sort(peaks, Peak.getIntensityComp())
            scala.util.Sorting.quickSort(peaks)(Peak.getIntensityOrdering())

            xicPeaks += peaks(peaksCount - 1)
          }

          i += 1
        }
      case XicMethod.NEAREST =>
        var i = 0
        while (i < spectrumSlicesCount) {
          val sl = spectrumSlices(i)
          val slData = sl.getData
          if (!slData.isEmpty) {
            val nearestPeak = sl.getNearestPeak(searchedMz, mzTolPPM)
            if (nearestPeak == null) {
              //this.logger.error("nearest peak is null but should not be: searchedMz=" + searchedMz + " minMz=" + slData.getMzList(0) + " tol=" + mzTolPPM)
            } else {
              xicPeaks += nearestPeak
            }
          }

          i += 1
        }
      case XicMethod.SUM =>
        var i = 0
        while (i < spectrumSlicesCount) {
          val sl = spectrumSlices(i)
          val peaks = sl.toPeaks()
          val peaksCount = peaks.length

          if (peaksCount != 0) {
            //java.util.Arrays.sort(peaks, Peak.getIntensityComp())
            scala.util.Sorting.quickSort(peaks)(Peak.getIntensityOrdering())

            var sum = 0.0f
            for (p <- peaks) {
              sum += p.getIntensity
            }

            val refPeak = peaks(Math.floor(0.5 * peaksCount).toInt)
            xicPeaks += Peak(refPeak.getMz, sum, refPeak.getLeftHwhm, refPeak.getRightHwhm, refPeak.getLcContext)
          }

          i += 1
        }
      case _ => throw new Exception("[_spectrumSlicesToXIC]: method must be one of 'MAX', 'NEAREST' or 'SUM'")
    }

    xicPeaks.toArray
  }

  protected def getMsPeaksInMzRtRanges(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float)(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    val spectrumSlices = this.getMsSpectrumSlices(minMz, maxMz, minRt, maxRt)
    this._spectrumSlicesToPeaks(spectrumSlices)
  }

  protected def getMsnPeaksInMzRtRanges(parentMz: Double, minFragMz: Double, maxFragMz: Double, minRt: Float, maxRt: Float)(implicit mzDbCtx: MzDbContext): Array[IPeak] = {
    val spectrumSlices = this.getMsnSpectrumSlices(parentMz, minFragMz, maxFragMz, minRt, maxRt)
    this._spectrumSlicesToPeaks(spectrumSlices)
  }

  /** Merge spectrum slices then return a peak array using simply the toPeaks function **/
  private def _spectrumSlicesToPeaks(spectrumSlices: Array[SpectrumSlice]): Array[IPeak] = {
    //this.logger.debug("SpectrumSlice length : {}", spectrumSlices.length)

    if (spectrumSlices.length == 0) return Array.empty[IPeak]

    var mergedPeaksCount = 0
    for (spectrumSlice <- spectrumSlices) {
      val sd = spectrumSlice.getData
      mergedPeaksCount += sd.peaksCount
    }

    val peaks = new Array[IPeak](mergedPeaksCount)
    var peakIdx = 0
    for (spectrumSlice <- spectrumSlices) {
      for (peak <- spectrumSlice.toPeaks()) {
        peaks(peakIdx) = peak
        peakIdx += 1
      }
    }

    peaks
  }
}

object XicMethod extends Enumeration {
  val MAX, NEAREST, SUM = Value
}
