package com.github.sqlite4s

import java.io.File

trait ISQLiteConnection extends Any {
  //def connectionHandle(): SQLiteConnection.Handle
  def getDatabaseFile(): File
  def isMemoryDatabase(): Boolean
  def setStepsPerCallback(stepsPerCallback: Int): Unit
  def setLimit(id: Int, newVal: Int): Int
  def getLimit(id: Int): Int
  def open(allowCreate: Boolean): ISQLiteConnection
  def open(): ISQLiteConnection
  def openReadonly(): ISQLiteConnection
  def openV2(flags: Int): ISQLiteConnection
  def isOpen(): Boolean
  def isDisposed: Boolean
  def isReadOnly(dbName: String): Boolean
  def flush(): Unit
  def safeFlush(): Unit
  def isReadOnly(): Boolean
  def getOpenFlags(): Int
  def dispose(): Unit
  def exec(sql: String): ISQLiteConnection
  //def getTableColumnMetadata(dbName: String, tableName: String, columnName: String): SQLiteColumnMetadata
  //def prepare(sql: SQLParts, cached: Boolean, flags: Int): SQLiteStatement
  def prepare(sql: String): ISQLiteStatement
  def prepare(sql: String, flags: Int): ISQLiteStatement
  def prepare(sql: String, cached: Boolean): ISQLiteStatement
  def prepare(sql: String, cached: Boolean, flags: Int): ISQLiteStatement
  //def prepare(sql: SQLParts): SQLiteStatement
  //def prepare(sql: SQLParts, cached: Boolean): SQLiteStatement
  //def prepare(sql: SQLParts, flags: Int): SQLiteStatement
  def blob(table: String, column: String, rowid: Long, writeAccess: Boolean): ISQLiteBlob
  def getAutoCommit(): Boolean
  def getLastInsertId(): Long
  //def getStatementCount(): Int
}

trait ISQLiteStatement extends Any {

  def isDisposed(): Boolean

  def dispose(): Unit

  def reset(clearBindings: Boolean): ISQLiteStatement
  def reset(): ISQLiteStatement

  def step(): Boolean

  def hasRow: Boolean

  def loadInts(column: Int, buffer: Array[Int], offset: Int, length: Int): Int
  def loadLongs(column: Int, buffer: Array[Long], offset: Int, length: Int): Int

  def bind(index: Int, value: Double): ISQLiteStatement
  def bind(name: String, value: Double): ISQLiteStatement
  def bind(index: Int, value: Int): ISQLiteStatement
  def bind(name: String, value: Int): ISQLiteStatement
  def bind(index: Int, value: Long): ISQLiteStatement
  def bind(name: String, value: Long): ISQLiteStatement
  def bind(index: Int, value: String): ISQLiteStatement
  def bind(name: String, value: String): ISQLiteStatement
  def bind(index: Int, value: Array[Byte]): ISQLiteStatement
  def bind(name: String, value: Array[Byte]): ISQLiteStatement
  def bind(index: Int, value: Array[Byte], offset: Int, length: Int): ISQLiteStatement
  def bind(name: String, value: Array[Byte], offset: Int, length: Int): ISQLiteStatement
  def bindZeroBlob(index: Int, length: Int): ISQLiteStatement
  def bindZeroBlob(name: String, length: Int): ISQLiteStatement
  def bindNull(index: Int): ISQLiteStatement
  def bindNull(name: String): ISQLiteStatement

  def columnString(column: Int): String
  def columnInt(column: Int): Int
  def columnDouble(column: Int): Double
  def columnLong(column: Int): Long
  def columnBlob(column: Int): Array[Byte]
  def columnNull(column: Int): Boolean
  def columnCount(): Int

  def getColumnName(column: Int): String
}

trait ISQLiteBlob extends Any {
  def getSize(): Int
  def dispose(): Unit
  def isDisposed(): Boolean
  def read(blobOffset: Int, buffer: Array[Byte], offset: Int, length: Int): Unit
  def write(blobOffset: Int, buffer: Array[Byte], offset: Int, length: Int): Unit
  def isWriteAllowed: Boolean
  def reopen(rowid: Long): Boolean
}

trait ISQLiteConnectionFactory {
  def newSQLiteConnection(): ISQLiteConnection
}

trait ISQLiteExceptionFactory {
  def newSQLiteException(errorCode: Int, errorMessage: String): Exception
}

trait ISQLiteFactory extends ISQLiteConnectionFactory with ISQLiteExceptionFactory {
  //type SQLiteConnectionType <: ISQLiteConnection
  //type SQLiteStatementType <: ISQLiteStatement

  //def newSQLiteStatement(): SQLiteStatementType
  def newSQLiteConnection(): ISQLiteConnection
  def newSQLiteConnection(dbFile: java.io.File): ISQLiteConnection

  def newSQLiteException(errorCode: Int, errorMessage: String): Exception

  def configureLogging(logLevel: com.github.mzdb4s.LogLevel): Unit
}

/*object SQLiteFactory extends ISQLiteFactory {
  def newSQLiteConnection(): ISQLiteConnection = null
}*/

/*trait AbstractSQLiteFactory extends ISQLiteFactory {
  //def newSQLiteConnection(): ISQLiteConnection = null
}
*/