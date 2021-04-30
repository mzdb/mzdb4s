package com.github.mzdb4s.io.timsdata

import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.Logging
import com.github.sqlite4s.{ISQLiteConnection, ISQLiteFactory, SQLiteQuery}

class TDFMetaDataReader(
  val dirPath: String,
  val logConnections: Boolean = false
)(implicit sf: ISQLiteFactory) extends AutoCloseable with Logging {

  // Disable logs for JVM implementation
  // TODO: do the same for Native implementation
  if (!logConnections) {
    sf.configureLogging(com.github.sqlite4s.LogLevel.OFF)
  }

  // Check if database exists
  private val _dir = new java.io.File(dirPath)
  require(_dir.isDirectory, "can't find file at: " + dirPath)
  private val _file = new java.io.File(_dir, "analysis.tdf")
  require(_file.isFile, "can't find file 'analysis.tdf' file at:" + _file)

  private var _connectionOpt: Option[ISQLiteConnection] = None

  def open(): Unit = {
    assert(_connectionOpt.isEmpty, "connection to TDF file already opened")

    val connection = sf.newSQLiteConnection(_file)

    connection.openReadonly()

    // SQLite optimization
    connection.exec("PRAGMA synchronous=OFF;")
    connection.exec("PRAGMA journal_mode=OFF;")
    connection.exec("PRAGMA temp_store=2;")
    connection.exec("PRAGMA cache_size=-100000;") // around 100 Mo
    connection.exec("PRAGMA mmap_size=2147418112;") // around 2 GB of mapped-memory (it may help for batch processing)

    _connectionOpt = Some(connection)
  }

  def close(): Unit = {
    assert(_connectionOpt.isDefined, "connection to TDF file already closed")

    _connectionOpt.get.dispose()

    _connectionOpt = None
  }

  @inline
  private def _newSQLiteQuery(
    sqlQuery: String,
    cacheStmt: Boolean = true
  ): SQLiteQuery = {
    new SQLiteQuery(_connectionOpt.get, sqlQuery, cacheStmt)
  }

  def getFramesCount(): Int = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")
    _newSQLiteQuery("SELECT count(*) FROM Frames").extractSingleInt()
  }

  def getGlobalMetaData(): scala.collection.Map[String,String] = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")

    val query = _newSQLiteQuery("SELECT Key, Value FROM GlobalMetadata")

    // Note: we use the statement API to use index based access
    val stmt = query.getStatement

    val metaData = new collection.mutable.HashMap[String,String]()
    query.forEachRecord( { (record, idx) =>
      val k = stmt.columnString(0)
      val v = stmt.columnString(1)
      metaData.put(k, v)
    })

    metaData
  }

  def getAllPrecursors(): collection.Seq[TDFPrecursor] = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")

    val query = _newSQLiteQuery(
      "SELECT Id, LargestPeakMz, AverageMz, MonoisotopicMz, Charge, ScanNumber, Intensity, Parent FROM Precursors"
    )

    // Note: we use the statement API to use index based access
    val stmt = query.getStatement

    val precursors = new ArrayBuffer[TDFPrecursor]()
    query.forEachRecord( { (record, idx) =>
      val precId = stmt.columnLong(0)
      val largestPeakMz = stmt.columnDouble(1)
      val avgMz = stmt.columnDouble(2)
      val monoIsotopMz = stmt.columnDouble(3)
      val charge = stmt.columnInt(4)
      val scanNbr = stmt.columnDouble(5)
      val intensity = stmt.columnDouble(6).toFloat
      val parentFr = stmt.columnInt(7)
      precursors += TDFPrecursor(precId, largestPeakMz, avgMz, monoIsotopMz, charge, scanNbr, intensity, parentFr)
    })

    precursors
  }

  def getAllFrames(): collection.Seq[TDFFrame] = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")

    val sql = "SELECT Id, Time, Polarity, ScanMode, MsMsType, TimsId, MaxIntensity, NumScans, MaxIntensity, " +
      "SummedIntensities, NumScans, NumPeaks, MzCalibration, T1, T2, TimsCalibration, PropertyGroup, AccumulationTime, RampTime "+
      "FROM Frames"
    val query = _newSQLiteQuery(sql)

    val frames = new ArrayBuffer[TDFFrame](this.getFramesCount())

    // Note: we use the statement API to use index based access
    val stmt = query.getStatement

    query.forEachRecord( { (record, idx) =>
      frames += TDFFrame(
        id = stmt.columnLong(0),
        time = stmt.columnDouble(1).toFloat,
        polarity = stmt.columnString(2), // can be ('+', '-')
        scanMode = ScanMode.withValue(stmt.columnInt(3)),
        msType = MsType.withValue(stmt.columnInt(4)),
        timsId = if (stmt.columnNull(5)) None else Some(stmt.columnInt(5)),
        maxIntensity = stmt.columnInt(6),
        summedIntensities = stmt.columnInt(7),
        numScans = stmt.columnInt(8),
        numPeaks = stmt.columnInt(9),
        mzCalibration = stmt.columnInt(10),
        t1 = stmt.columnDouble(11),
        t2 = stmt.columnDouble(12),
        timsCalibration = stmt.columnInt(13),
        propertyGroup = if (stmt.columnNull(14)) None else Some(stmt.columnInt(14)),
        accumulationTime = stmt.columnDouble(15).toFloat,
        rampTime = stmt.columnDouble(16).toFloat
      )
    })

    frames
  }

  def getAllMsMsScans(): collection.Seq[TDFMsMsScan] = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")

    val sql = "SELECT Frame, Parent, TriggerMass, IsolationWidth, PrecursorCharge, CollisionEnergy FROM FrameMsMsInfo"
    val query = _newSQLiteQuery(sql)

    val msmsScans = new ArrayBuffer[TDFMsMsScan]()

    // Note: we use the statement API to use index based access
    val stmt = query.getStatement

    query.forEachRecord( { (record, idx) =>
      msmsScans += TDFMsMsScan(
        frameId = stmt.columnLong(0),
        parentFrameId = stmt.columnLong(1),
        triggeredMass = stmt.columnDouble(2),
        isolationWidth = stmt.columnDouble(3).toFloat,
        precursorCharge = if (stmt.columnNull(4)) None else Some(stmt.columnInt(4)),
        collisionEnergy = stmt.columnDouble(5).toFloat
      )
    })

    msmsScans
  }

  def getAllPasefMsMsScans(): collection.Seq[TDFPasefMsMsScan] = {
    assert(_connectionOpt.isDefined, "connection to TDF file is closed")

    val sql = "SELECT Frame, ScanNumBegin, ScanNumEnd, IsolationMz, IsolationWidth, CollisionEnergy, Precursor FROM PasefFrameMsMsInfo"
    val query = _newSQLiteQuery(sql)

    val pasefScans = new ArrayBuffer[TDFPasefMsMsScan]()

    // Note: we use the statement API to use index based access
    val stmt = query.getStatement

    query.forEachRecord( { (record, idx) =>
      pasefScans += TDFPasefMsMsScan(
        frameId = stmt.columnLong(0),
        scanMin = stmt.columnInt(1),
        scanMax = stmt.columnInt(2),
        isolationMz = stmt.columnDouble(3),
        isolationWidth = stmt.columnDouble(4).toFloat,
        collisionEnergy = stmt.columnDouble(5).toFloat,
        precursorId = if (stmt.columnNull(6)) None else Some(stmt.columnLong(6))
      )
    })

    pasefScans
  }

}

case class TDFPrecursor(
  id: Long,
  largestPeakMz: Double,
  averageMz: Double,
  monoIsotopicMz: Double,
  charge: Int,
  scanNumber: Double,
  intensity: Float,
  parentFrameId: Int
)

object MsType extends Enumeration {
  val MS = Value(0)
  val MSMS = Value(2)
  val PASEF = Value(8)
  val DIA = Value(9)

  def withValue(v: Int): MsType.Value = {
    v match {
      case 0 => MS
      case 2 => MSMS
      case 8 => PASEF
      case 9 => DIA
      case _ => throw new Exception(s"No defined MsType of value '$v'")
    }
  }
}

object ScanMode extends Enumeration {
  val MS = Value(0)
  val AUTO_MSMS = Value(1)
  val MRM = Value(2)
  val IN_SOURCE_CID = Value(3)
  val BROADBAND_CID = Value(4)
  val PASEF = Value(8)
  val DIA = Value(9)

  def withValue(v: Int): ScanMode.Value = {
    v match {
      case 0 => MS
      case 1 => AUTO_MSMS
      case 2 => MRM
      case 3 => IN_SOURCE_CID
      case 4 => BROADBAND_CID
      case 8 => PASEF
      case 9 => DIA
      case _ => throw new Exception(s"No defined ScanMode of value '$v'")
    }
  }
}

case class TDFFrame(
  id: Long,
  time: Float,
  polarity: String,
  scanMode: ScanMode.Value,
  msType: MsType.Value,
  timsId: Option[Int],
  maxIntensity: Int,
  summedIntensities: Int,
  numScans: Int,
  numPeaks: Int,
  mzCalibration: Int,
  t1: Double,
  t2: Double,
  timsCalibration: Int,
  propertyGroup: Option[Int],
  accumulationTime: Float,
  rampTime: Float
)

case class TDFMsMsScan(
  frameId: Long,
  parentFrameId: Long,
  triggeredMass: Double,
  isolationWidth: Float,
  precursorCharge: Option[Int],
  collisionEnergy: Float
)

case class TDFPasefMsMsScan(
  frameId: Long,
  scanMin: Int,
  scanMax: Int,
  isolationMz: Double,
  isolationWidth: Float,
  collisionEnergy: Float,
  precursorId: Option[Long]
)