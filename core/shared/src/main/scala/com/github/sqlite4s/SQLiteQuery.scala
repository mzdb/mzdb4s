package com.github.sqlite4s

import java.util.Arrays

import scala.collection.mutable.ArrayBuffer

import com.github.sqlite4s.query._


/**
  * @author David Bouyssie
  *
  */
class SQLiteQuery(
  val connection: ISQLiteConnection,
  val sqlQuery: String,
  val cacheStmt: Boolean
)(implicit val sqliteErrorFactory: ISQLiteExceptionFactory) {

  private var stmt = connection.prepare(sqlQuery, cacheStmt)

  private val resultDesc = {
    val colIdxByColName = new collection.mutable.HashMap[String, Integer]

    val nbCols: Int = stmt.columnCount()

    var colIdx = 0
    while (colIdx < nbCols) {
      val colName = stmt.getColumnName(colIdx)
      colIdxByColName.put(colName, colIdx)
      colIdx += 1
    }

    new SQLiteResultDescriptor(colIdxByColName)
  }

  def this(connection: ISQLiteConnection, sqlQuery: String)(implicit sqliteErrorFactory: ISQLiteExceptionFactory) {
    this(connection, sqlQuery, true)
  }

  def getColumnIndex(colName: String): Int = {
    if (! resultDesc.colIdxByColName.contains(colName))
      throw sqliteErrorFactory.newSQLiteException(-1, s"undefined column '$colName' in query: $sqlQuery")

    resultDesc.colIdxByColName(colName)
  }

  def getColumnNames(): Seq[String] = this.resultDesc.getColumnNames()

  def getStatement: ISQLiteStatement = stmt

  def dispose(): Unit = if (! this.isStatementDisposed) {
    this.stmt.dispose()
    this.stmt = null
  }

  @inline
  def isStatementDisposed: Boolean = if (this.stmt == null || this.stmt.isDisposed) true else false

  private def _checkIsNotDisposed(): Unit = {
    if (isStatementDisposed)
      throw new IllegalStateException("SQLite statement is already disposed")
  }

  def bind(index: Int, value: Double): SQLiteQuery = {
    this.stmt.bind(index, value)
    this
  }

  def bind(index: Int, value: Int): SQLiteQuery = {
    this.stmt.bind(index, value)
    this
  }

  def bind(index: Int, value: Long): SQLiteQuery = {
    this.stmt.bind(index, value)
    this
  }

  def bind(index: Int, value: String): SQLiteQuery = {
    this.stmt.bind(index, value)
    this
  }

  def bind(index: Int, value: Array[Byte]): SQLiteQuery = {
    this.stmt.bind(index, value)
    this
  }

  def bind(index: Int, value: Array[Byte], offset: Int, length: Int): SQLiteQuery = {
    this.stmt.bind(index, value, offset, length)
    this
  }

  def bindZeroBlob(index: Int, length: Int): SQLiteQuery = {
    this.stmt.bindZeroBlob(index, length)
    this
  }

  def bindNull(index: Int): SQLiteQuery = {
    this.stmt.bindNull(index)
    this
  }

  //def bindStream(index: Int): OutputStream = this.stmt.bindStream(index, 0)

  //def bindStream(index: Int, bufferSize: Int): OutputStream = this.stmt.bindStream(index, bufferSize)

  def getRecordIterator(): SQLiteRecordIterator = {
    _checkIsNotDisposed()
    new SQLiteRecordIterator(this)
  }

  def forEachRecord(recordAndIndexFn: (SQLiteRecord, Int) => Unit ): Unit = {
    _checkIsNotDisposed()

    var idx = 0
    while (this.stmt.step()) {
      recordAndIndexFn(new SQLiteRecord(this), idx)
      idx += 1
    }

    // Dispose the statement
    this.dispose()
  }

  def extractRecords[T](extractRecord: SQLiteRecord => T, records: Array[T]): Array[T] = {
    val extractor = new Object with ISQLiteRecordExtraction[T] {
      def extractRecord(record: SQLiteRecord): T = extractRecord(record)
    }
    this.extractRecords(extractor, records)
  }

  def extractRecords[T](extractor: ISQLiteRecordExtraction[T], records: Array[T]): Array[T] = {
    _checkIsNotDisposed()

    val recordsCount = records.length
    var idx = 0
    while (this.stmt.step && idx < recordsCount) {
      records(idx) = extractor.extractRecord(new SQLiteRecord(this))
      idx += 1
    }
    this.dispose()

    records
  }

  def extractRecordList[T](extractor: ISQLiteRecordExtraction[T]): collection.Seq[T] = {
    _checkIsNotDisposed()

    val records = new ArrayBuffer[T]

    while (this.stmt.step())
      records += extractor.extractRecord(new SQLiteRecord(this))

    this.dispose()

    records
  }

  def extractRecord[T](extractor: ISQLiteRecordExtraction[T]): T = {
    _checkIsNotDisposed()

    this.stmt.step()
    val obj = extractor.extractRecord(new SQLiteRecord(this))
    this.dispose()

    obj
  }

  def extractStrings(bufferLength: Int): Array[String] = {
    _checkIsNotDisposed()

    val buffer = new Array[String](bufferLength)
    var idx = 0
    while (this.stmt.step()) {
      buffer(idx) = this.stmt.columnString(0)
      idx += 1
    }

    this.dispose()

    Arrays.copyOfRange(buffer, 0, idx - 1)
  }


  def extractInts(bufferLength: Int): Array[Int] = {
    _checkIsNotDisposed()

    val buffer = new Array[Int](bufferLength)
    val loadedInts = this.stmt.loadInts(0, buffer, 0, bufferLength)
    this.dispose()

    Arrays.copyOfRange(buffer, 0, loadedInts - 1)
  }

  def extractLongs(bufferLength: Int): Array[Long] = {
    _checkIsNotDisposed()

    val buffer = new Array[Long](bufferLength)
    val loadedLongs = this.stmt.loadLongs(0, buffer, 0, bufferLength)
    this.dispose()

    Arrays.copyOfRange(buffer, 0, loadedLongs - 1)
  }

  def extractFloats(bufferLength: Int): Array[Float] = {
    _checkIsNotDisposed()

    val buffer = new Array[Float](bufferLength)
    var idx = 0
    while (this.stmt.step()) {
      buffer(idx) = this.stmt.columnDouble(0).asInstanceOf[Float]
      idx += 1
    }
    this.dispose()

    Arrays.copyOfRange(buffer, 0, idx - 1)
  }


  def extractDoubles(bufferLength: Int): Array[Double] = {
    _checkIsNotDisposed()

    var buffer: Array[Double] = null
    buffer = new Array[Double](bufferLength)

    var idx = 0
    while (this.stmt.step()) {
      buffer(idx) = this.stmt.columnDouble(0)
      idx += 1
    }
    this.dispose()

    Arrays.copyOfRange(buffer, 0, idx - 1)
  }

  def extractSingleString(): String = {
    _checkIsNotDisposed()

    this.stmt.step()
    val result = this.stmt.columnString(0)
    this.dispose()

    result
  }

  def extractSingleInt(): Int = {
    _checkIsNotDisposed()

    this.stmt.step()
    val result = this.stmt.columnInt(0)
    this.dispose()

    result
  }

  def extractSingleLong(): Long = {
    _checkIsNotDisposed()

    this.stmt.step()
    val result = this.stmt.columnLong(0)
    this.dispose()

    result
  }

  def extractSingleDouble(): Double = {
    this.stmt.step()
    val result = this.stmt.columnDouble(0)
    this.dispose()
    result
  }

  def extractSingleBlob(): Array[Byte] = {
    _checkIsNotDisposed()

    this.stmt.step()
    if (!stmt.hasRow) return null
    val result = this.stmt.columnBlob(0)
    this.dispose()

    result
  }

  /*def extractSingleInputStream(): InputStream = {
    _checkIsNotDisposed()

    this.stmt.step
    val result = this.stmt.columnStream(0)
    this.dispose()

    result
  }*/
}

/*trait ISQLiteRecordOperation {
  def execute(elem: SQLiteRecord, idx: Int): Unit
}*/

trait ISQLiteRecordExtraction[T] {
  def extractRecord(record: SQLiteRecord): T
}

class SQLiteResultDescriptor(
  val colIdxByColName: collection.Map[String, Integer]
) {
  //def getColIdxByColName(): HashMap[String, Integer] = colIdxByColName

  def getColumnIndex(colName: String): Int = this.colIdxByColName(colName)

  def getColumnNames(): Seq[String] = this.colIdxByColName.keys.toList
}