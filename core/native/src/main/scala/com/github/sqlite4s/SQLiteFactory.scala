package com.github.sqlite4s

import java.io.File

trait SQLiteConnectionFactory extends ISQLiteConnectionFactory {
  implicit def jvmConn2wrapper(conn: SQLiteConnection): ISQLiteConnection = new SQLiteConnectionWrapper(conn)

  def newSQLiteConnection(): ISQLiteConnection = new SQLiteConnection()
  def newSQLiteConnection(dbFile: File): ISQLiteConnection = new SQLiteConnection(dbFile)
}

trait SQLiteExceptionFactory extends ISQLiteExceptionFactory {
  def newSQLiteException(errorCode: Int, errorMessage: String): Exception = new SQLiteException(errorCode, errorMessage)
}

object SQLiteFactory extends ISQLiteFactory with SQLiteConnectionFactory with SQLiteExceptionFactory {
  // Implicit conversion for proxy objects
  implicit def jvmStmt2wrapper(stmt: SQLiteStatement): ISQLiteStatement = new SQLiteStatementWrapper(stmt)
  implicit def nativeBlob2wrapper(blob: SQLiteBlob): ISQLiteBlob = new SQLiteBlobWrapper(blob)

  def configureLogging(logLevel: LogLevel): Unit = {
    Logging.configureLogger(logLevel)

  }
}

class SQLiteConnectionWrapper(val conn: SQLiteConnection) extends AnyVal with ISQLiteConnection {

  import SQLiteFactory._

  //def connectionHandle(): SQLiteConnection.Handle
  @inline def getDatabaseFile(): File = conn.getDatabaseFile
  @inline def isMemoryDatabase(): Boolean = conn.isMemoryDatabase
  @inline def setStepsPerCallback(stepsPerCallback: Int): Unit = conn.isMemoryDatabase
  @inline def setLimit(id: Int, newVal: Int): Int = conn.setLimit(id, newVal)
  @inline def getLimit(id: Int): Int = conn.getLimit(id)
  @inline def open(allowCreate: Boolean): ISQLiteConnection = conn.open(allowCreate)
  @inline def open(): ISQLiteConnection = conn.open()
  @inline def openReadonly(): ISQLiteConnection = conn.openReadonly()
  @inline def openV2(flags: Int): ISQLiteConnection = conn.openV2(flags)
  @inline def isOpen(): Boolean = conn.isOpen
  @inline def isDisposed: Boolean = conn.isDisposed
  @inline def isReadOnly(dbName: String): Boolean = conn.isReadOnly(dbName)
  @inline def flush(): Unit = conn.flush()
  @inline def safeFlush(): Unit = conn.safeFlush()
  @inline def isReadOnly(): Boolean = conn.isReadOnly
  @inline def getOpenFlags(): Int = conn.getOpenFlags
  @inline def dispose(): Unit = conn.dispose()
  @inline def exec(sql: String): ISQLiteConnection = conn.exec(sql)
  //def getTableColumnMetadata(dbName: String, tableName: String, columnName: String): SQLiteColumnMetadata
  //def prepare(sql: SQLParts, cached: Boolean, flags: Int): SQLiteStatement
  @inline def prepare(sql: String): ISQLiteStatement = conn.prepare(sql)
  @inline def prepare(sql: String, flags: Int): ISQLiteStatement = conn.prepare(sql, flags)
  @inline def prepare(sql: String, cached: Boolean): ISQLiteStatement = conn.prepare(sql, cached)
  @inline def prepare(sql: String, cached: Boolean, flags: Int): ISQLiteStatement = conn.prepare(sql, cached, flags)
  //def prepare(sql: SQLParts): SQLiteStatement
  //def prepare(sql: SQLParts, cached: Boolean): SQLiteStatement
  //def prepare(sql: SQLParts, flags: Int): SQLiteStatement
  @inline def blob(table: String, column: String, rowid: Long, writeAccess: Boolean): ISQLiteBlob = {
    conn.blob(table, column, rowid, writeAccess)
  }
  @inline def getAutoCommit(): Boolean = conn.getAutoCommit()
  @inline def getLastInsertId(): Long = conn.getLastInsertId()
  //@inline def getStatementCount(): Int = conn.getChanges // FIXME
}

class SQLiteStatementWrapper(val stmt: SQLiteStatement) extends AnyVal with ISQLiteStatement {

  import SQLiteFactory._

  @inline def isDisposed(): Boolean = stmt.isDisposed

  @inline def dispose(): Unit = stmt.dispose()

  @inline def reset(): ISQLiteStatement = stmt.reset()
  @inline def reset(clearBindings: Boolean): ISQLiteStatement = stmt.reset()

  @inline def step(): Boolean = stmt.step()

  @inline def hasRow: Boolean = stmt.hasRow

  @inline def loadInts(column: Int, buffer: Array[Int], offset: Int, length: Int): Int = stmt.loadInts(column,buffer,offset,length)
  @inline def loadLongs(column: Int, buffer: Array[Long], offset: Int, length: Int): Int = stmt.loadLongs(column,buffer,offset,length)

  @inline def bind(index: Int, value: Double): ISQLiteStatement = stmt.bind(index, value)
  @inline def bind(name: String, value: Double): ISQLiteStatement = stmt.bind(name, value)
  @inline def bind(index: Int, value: Int): ISQLiteStatement = stmt.bind(index, value)
  @inline def bind(name: String, value: Int): ISQLiteStatement = stmt.bind(name, value)
  @inline def bind(index: Int, value: Long): ISQLiteStatement = stmt.bind(index, value)
  @inline def bind(name: String, value: Long): ISQLiteStatement = stmt.bind(name, value)
  @inline def bind(index: Int, value: String): ISQLiteStatement = stmt.bind(index, value)
  @inline def bind(name: String, value: String): ISQLiteStatement = stmt.bind(name, value)
  @inline def bind(index: Int, value: Array[Byte]): ISQLiteStatement = stmt.bind(index, value)
  @inline def bind(name: String, value: Array[Byte]): ISQLiteStatement = stmt.bind(name, value)
  @inline def bind(index: Int, value: Array[Byte], offset: Int, length: Int): ISQLiteStatement = stmt.bind(index, value, offset, length)
  @inline def bind(name: String, value: Array[Byte], offset: Int, length: Int): ISQLiteStatement = stmt.bind(name, value, offset, length)
  @inline def bindZeroBlob(index: Int, length: Int): ISQLiteStatement = stmt.bindZeroBlob(index, length)
  @inline def bindZeroBlob(name: String, length: Int): ISQLiteStatement = stmt.bindZeroBlob(name, length)
  @inline def bindNull(index: Int): ISQLiteStatement = stmt.bindNull(index)
  @inline def bindNull(name: String): ISQLiteStatement = stmt.bindNull(name)

  @inline def columnString(column: Int): String = stmt.columnString(column)
  @inline def columnInt(column: Int): Int = stmt.columnInt(column)
  @inline def columnDouble(column: Int): Double = stmt.columnDouble(column)
  @inline def columnLong(column: Int): Long = stmt.columnLong(column)
  @inline def columnBlob(column: Int): Array[Byte] = stmt.columnBlob(column)
  @inline def columnNull(column: Int): Boolean = stmt.columnNull(column)
  @inline def columnCount(): Int = stmt.columnCount()

  @inline def getColumnName(column: Int): String = stmt.getColumnName(column)
}

class SQLiteBlobWrapper(val blob: SQLiteBlob) extends AnyVal with ISQLiteBlob {
  @inline def getSize(): Int = blob.getSize()
  @inline def dispose(): Unit = blob.dispose()
  @inline def isDisposed(): Boolean = blob.isDisposed()
  @inline def read(blobOffset: Int, buffer: Array[Byte], offset: Int, length: Int): Unit = {
    blob.read(blobOffset, buffer, offset, length)
  }
  @inline def write(blobOffset: Int, buffer: Array[Byte], offset: Int, length: Int): Unit = {
    blob.write(blobOffset, buffer, offset, length)
  }
  @inline def isWriteAllowed: Boolean = blob.isWriteAllowed
  @inline def reopen(rowid: Long): Boolean = {
    blob.reopen(rowid)
    true
  }
}