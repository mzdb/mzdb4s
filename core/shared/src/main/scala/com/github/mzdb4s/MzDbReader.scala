package com.github.mzdb4s

import java.io.{File, FileInputStream, FileNotFoundException}

import scala.collection.mutable.LongMap
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.MzDbReaderQueries
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.io.reader.iterator._
import com.github.mzdb4s.io.reader.table._
import com.github.mzdb4s.msdata._
import com.github.sqlite4s._

/**
  * Allows to manipulates data contained in the mzDB file.
  *
  * @author David Bouyssié
  */
object MzDbReader {
  protected def readWholeFile(file: File): Unit = {
    val fis = new FileInputStream(file)
    val b = new Array[Byte](1024 * 1024)
    while (fis.available != 0) fis.read(b)
    fis.close()
  }
}

class MzDbReader(
  val dbFile: File,
  val entityCache: Option[MzDbEntityCache],
  val logConnections: Boolean
)(implicit sf: ISQLiteFactory) extends AbstractMzDbReader {

  // Disable logs for JVM implementation
  // TODO: do the same for Native implementation
  if (!logConnections) {
    sf.configureLogging(com.github.sqlite4s.LogLevel.OFF)
  }

  // Check if database exists
  if (!dbFile.exists)
    throw new FileNotFoundException(s"can't find the mzDB file at the given path: $dbFile")

  private val _connection = sf.newSQLiteConnection(dbFile)
  private implicit val _mzDbCtx: MzDbContext = new MzDbContext(_connection)(sf)

  this._connection.openReadonly()

  // SQLite optimization
  this._connection.exec("PRAGMA synchronous=OFF;")
  this._connection.exec("PRAGMA journal_mode=OFF;")
  this._connection.exec("PRAGMA temp_store=2;")
  this._connection.exec("PRAGMA cache_size=-100000;") // around 100 Mo
  this._connection.exec("PRAGMA mmap_size=2147418112;") // around 2 GB of mapped-memory (it may help for batch processing)

  // Create a temporary table containing a copy of the sepctrum table
  // System.out.println("before CREATE TEMP TABLE");
  // connection.exec("CREATE TEMP TABLE tmp_spectrum AS SELECT * FROM spectrum");
  // System.out.println("after CREATE TEMP TABLE");

  /** Instantiates some readers without internal entity cache **/
  private lazy val _mzDbHeaderReader = new MzDbHeaderReader()
  private lazy val _instrumentConfigReader = new InstrumentConfigReader()
  private lazy val _runReader = new RunReader()
  private lazy val _sampleReader = new SampleReader()
  private lazy val _softwareListReader = new SoftwareReader()
  private lazy val _sourceFileReader = new SourceFileReader()

  // Set the mzDbHeader
  protected lazy val mzDbHeader: MzDbHeader = this._mzDbHeaderReader.getMzDbHeader()

  protected lazy val paramNameGetter: IMzDbParamName = {
    // Set the paramNameGetter
    val pwizMzDbVersion: String = MzDbReaderQueries.getPwizMzDbVersion()
    if (pwizMzDbVersion.compareTo("0.9.1") > 0) MzDbParamName_0_9 else MzDbParamName_0_8
  }

  /** Instantiates some readers with internal entity cache (entity cache object) **/
  private lazy val _dataEncodingReader = new DataEncodingReader(entityCache)
  private lazy val _spectrumHeaderReader = new SpectrumHeaderReader(this, _dataEncodingReader)
  private lazy val _runSliceHeaderReader = new RunSliceHeaderReader(this.bbSizes)

  def this(dbLocation: File, cacheEntities: Boolean)(implicit sf: ISQLiteFactory) {
    this(dbLocation, if (cacheEntities) Some(new MzDbEntityCache()) else None, false)
  }

  def this(dbPath: String, cacheEntities: Boolean)(implicit sf: ISQLiteFactory) {
    this(new File(dbPath), cacheEntities)
  }

  def getConnection(): ISQLiteConnection = this._connection

  def getMzDbContext(): MzDbContext = this._mzDbCtx

  def getMetaData(): MzDbMetaData = super.createMetaData(_dataEncodingReader.getDataEncodings())

  def getSpectraXmlMetaData(): Seq[SpectrumXmlMetaData] = super.getSpectraXmlMetaData()

  /**
    * Close the connection to avoid memory leaks.
    */
  override def close(): Unit = this._connection.dispose()

  override def getDataEncodingReader(): DataEncodingReader = _dataEncodingReader

  override def getSpectrumHeaderReader(): SpectrumHeaderReader = _spectrumHeaderReader

  override def getRunSliceHeaderReader(): RunSliceHeaderReader = _runSliceHeaderReader

  def getModelVersion(): String = MzDbReaderQueries.getModelVersion()

  def getPwizMzDbVersion(): String = MzDbReaderQueries.getPwizMzDbVersion()

  def getLastTime(): Float = MzDbReaderQueries.getLastTime()

  def getMaxMsLevel(): Int = MzDbReaderQueries.getMaxMsLevel()

  def getMzRange(msLevel: Int): Array[Int] = MzDbReaderQueries.getMzRange(msLevel)

  def getBoundingBoxesCount(): Int = MzDbReaderQueries.getBoundingBoxesCount()

  def getBoundingBoxesCount(runSliceId: Int): Int = MzDbReaderQueries.getBoundingBoxesCount(runSliceId)

  def getCyclesCount(): Int = MzDbReaderQueries.getCyclesCount()

  def getDataEncodingsCount(): Int = MzDbReaderQueries.getDataEncodingsCount()

  def getSpectraCount(): Int = MzDbReaderQueries.getSpectraCount()

  def getSpectraCount(msLevel: Int): Int = MzDbReaderQueries.getSpectraCount(msLevel)

  def getRunSlicesCount(): Int = MzDbReaderQueries.getRunSlicesCount()

  def getTableRecordsCount(tableName: String): Int = MzDbReaderQueries.getTableRecordsCount(tableName)

  def getDataEncoding(id: Int): DataEncoding = this._dataEncodingReader.getDataEncoding(id)

  def getDataEncodingBySpectrumId(): LongMap[DataEncoding] = this._dataEncodingReader.getDataEncodingBySpectrumId()

  def getSpectrumDataEncoding(spectrumId: Long): DataEncoding = this._dataEncodingReader.getSpectrumDataEncoding(spectrumId)

  def getRunSliceHeaders(msLevel: Int): Array[RunSliceHeader] = this._runSliceHeaderReader.getRunSliceHeaders(msLevel)

  def getRunSliceHeaderById(msLevel: Int): LongMap[RunSliceHeader] = this._runSliceHeaderReader.getRunSliceHeaderById(msLevel)

  def getRunSliceData(runSliceId: Int): RunSliceData = super.getRunSliceData(runSliceId)

  def getBoundingBoxData(bbId: Int): Array[Byte] = MzDbReaderQueries.getBoundingBoxData(bbId)

  def getBoundingBoxFirstSpectrumId(spectrumId: Long): Long = MzDbReaderQueries.getBoundingBoxFirstSpectrumId(spectrumId)

  def getBoundingBoxMinMz(bbId: Int): Float = MzDbReaderQueries.getBoundingBoxMinMz(bbId)

  def getBoundingBoxMinTime(bbId: Int): Float = MzDbReaderQueries.getBoundingBoxMinTime(bbId)

  def getBoundingBoxMsLevel(bbId: Int): Int = MzDbReaderQueries.getBoundingBoxMsLevel(bbId)

  def getMs1SpectrumHeaders(): Array[SpectrumHeader] = this._spectrumHeaderReader.getMs1SpectrumHeaders()

  def getMs1SpectrumHeaderById(): LongMap[SpectrumHeader] = this._spectrumHeaderReader.getMs1SpectrumHeaderById()

  def getMs2SpectrumHeaders(): Array[SpectrumHeader] = this._spectrumHeaderReader.getMs2SpectrumHeaders()

  def getMs2SpectrumHeaderById(): LongMap[SpectrumHeader] = this._spectrumHeaderReader.getMs2SpectrumHeaderById()

  def getSpectrumHeaders(): Array[SpectrumHeader] = this._spectrumHeaderReader.getSpectrumHeaders()

  def getSpectrumHeaderById(): LongMap[SpectrumHeader] = this._spectrumHeaderReader.getSpectrumHeaderById()

  def getSpectrumHeader(id: Long): SpectrumHeader = this._spectrumHeaderReader.getSpectrumHeader(id)

  def getSpectrumHeaderForTime(time: Float, msLevel: Int): SpectrumHeader = this._spectrumHeaderReader.getSpectrumHeaderForTime(time, msLevel)

  def getSpectrumData(spectrumId: Long): ISpectrumData = super.getSpectrumData(spectrumId)

  def getSpectrum(spectrumId: Long): Spectrum = super.getSpectrum(spectrumId)

  def getSpectrumPeaks(spectrumId: Int): Array[IPeak] = super.getSpectrumPeaks(spectrumId)

  def getMsSpectrumSlices(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float): Array[SpectrumSlice] = {
    super.getMsSpectrumSlices(minMz, maxMz, minRt, maxRt)
  }

  // TODO: think about msLevel > 2
  def getMsnSpectrumSlices(parentMz: Double, minFragMz: Double, maxFragMz: Double, minRt: Float, maxRt: Float): Array[SpectrumSlice] = {
    super.getMsnSpectrumSlices(parentMz, minFragMz, maxFragMz, minRt, maxRt)
  }

  def getBoundingBoxIterator(msLevel: Int): BoundingBoxIterator = {
    // TODO: try to use msn_rtree join instead (may be faster)
    val stmt = _connection.prepare("SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id AND spectrum.ms_level= ?", false)
    stmt.bind(1, msLevel)
    new BoundingBoxIterator(this._spectrumHeaderReader, this._dataEncodingReader, stmt, msLevel)
  }

  def getSpectrumIterator(): SpectrumIterator = {
    MzDbReader.readWholeFile(this.dbFile)
    new SpectrumIterator(this)
  }

  def getSpectrumIterator(msLevel: Int): SpectrumIterator = {
    MzDbReader.readWholeFile(this.dbFile)
    new SpectrumIterator(this, msLevel)
  }

  def getLcMsRunSliceIterator(): LcMsRunSliceIterator = { // First pass to load the index
    /*val fakeStmt = this._connection.prepare("SELECT data FROM bounding_box", false)
    while (fakeStmt.step()) {}
    fakeStmt.dispose()*/
    MzDbReader.readWholeFile(this.dbFile)
    new LcMsRunSliceIterator(this)
  }

  def getLcMsRunSliceIterator(minRunSliceMz: Double, maxRunSliceMz: Double): LcMsRunSliceIterator = {
    MzDbReader.readWholeFile(this.dbFile)
    new LcMsRunSliceIterator(this, minRunSliceMz, maxRunSliceMz)
  }

  def getLcMsnRunSliceIterator(minParentMz: Double, maxParentMz: Double): LcMsnRunSliceIterator = {
    /*val fakeStmt = this._connection.prepare("SELECT data FROM bounding_box", false)
    while (fakeStmt.step()) {}
    fakeStmt.dispose()*/
    MzDbReader.readWholeFile(this.dbFile)
    new LcMsnRunSliceIterator(this, minParentMz, maxParentMz)
  }

  def getLcMsnRunSliceIterator(minParentMz: Double, maxParentMz: Double, minRunSliceMz: Double, maxRunSliceMz: Double): LcMsnRunSliceIterator = {
    MzDbReader.readWholeFile(this.dbFile)
    new LcMsnRunSliceIterator(this, minParentMz, maxParentMz, minRunSliceMz, maxRunSliceMz)
  }

  def getAcquisitionMode(): AcquisitionMode.Value = super.getAcquisitionMode()

  def getDIAIsolationWindows(): Seq[IsolationWindow] = super.getDIAIsolationWindows()

  override def getInstrumentConfigurations(): Seq[InstrumentConfiguration] = {
    if (this.instrumentConfigs == null) this.instrumentConfigs = this._instrumentConfigReader.getInstrumentConfigList()
    this.instrumentConfigs
  }

  override def getRuns(): Seq[Run] = {
    if (this.runs == null) this.runs = this._runReader.getRunList()
    this.runs
  }

  override def getSamples(): Seq[Sample] = {
    if (this.samples == null) this.samples = this._sampleReader.getSampleList()
    this.samples
  }

  override def getSoftwareList(): Seq[Software] = {
    if (this.softwareList == null) this.softwareList = this._softwareListReader.getSoftwareList()
    this.softwareList
  }

  override def getSourceFiles(): Seq[SourceFile] = {
    if (this.sourceFiles == null) this.sourceFiles = this._sourceFileReader.getSourceFileList()
    this.sourceFiles
  }

  def getMsXicInMzRange(minMz: Double, maxMz: Double, method: XicMethod.Value): Array[IPeak] = super.getMsXicInMzRange(minMz, maxMz, method)

  def getMsXicInMzRtRanges(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float, method: XicMethod.Value): Array[IPeak] = super.getMsXicInMzRtRanges(minMz, maxMz, minRt, maxRt, method)

  def getMsXic(mz: Double, mzTolInDa: Double, minRt: Float, maxRt: Float, msLevel: Int, method: XicMethod.Value): Array[IPeak] = super.getMsXic(mz, mzTolInDa, minRt, maxRt, method)

  def getMsnXIC(parentMz: Double, fragmentMz: Double, fragmentMzTolInDa: Double, minRt: Float, maxRt: Float, method: XicMethod.Value): Array[IPeak] = super.getMsnXic(parentMz, fragmentMz, fragmentMzTolInDa, minRt, maxRt, method)

  def getMsPeaksInMzRtRanges(minMz: Double, maxMz: Double, minRt: Float, maxRt: Float): Array[IPeak] = super.getMsPeaksInMzRtRanges(minMz, maxMz, minRt, maxRt)

  def getMsnPeaksInMzRtRanges(parentMz: Double, minFragMz: Double, maxFragMz: Double, minRt: Float, maxRt: Float): Array[IPeak] = super.getMsnPeaksInMzRtRanges(parentMz, minFragMz, maxFragMz, minRt, maxRt)
}