package com.github.mzdb4s.io

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.io.reader.param.ParamTreeParser
import com.github.sqlite4s._

class MzDbContext(val mzDbConnection: ISQLiteConnection)(implicit val sf: ISQLiteExceptionFactory) {

  @inline
  def newSQLiteQuery(
    sqlQuery: String,
    cacheStmt: Boolean = true
  ): SQLiteQuery = {
    new SQLiteQuery(mzDbConnection, sqlQuery, cacheStmt)
  }

  def loadParamTree(tableName: String): ParamTree = {
    val sqlString = s"SELECT param_tree FROM $tableName"
    val paramTreeAsStr = new SQLiteQuery(mzDbConnection, sqlString).extractSingleString()
    ParamTreeParser.parseParamTree(paramTreeAsStr)
  }

  def loadParamTreeById[T](tableName: String, id: T): ParamTree = {
    val sqlString = s"SELECT param_tree FROM $tableName WHERE id = " + id
    val paramTreeAsStr = new SQLiteQuery(mzDbConnection, sqlString).extractSingleString()
    ParamTreeParser.parseParamTree(paramTreeAsStr)
  }

  def loadScanList(spectrumId: Long): ScanList = {
    val sqlString = "SELECT scan_list FROM spectrum WHERE id = ?"
    val scanListAsStr = new SQLiteQuery(mzDbConnection, sqlString).bind(1, spectrumId).extractSingleString()
    ParamTreeParser.parseScanList(scanListAsStr)
  }

  def loadPrecursorList(spectrumId: Long): Precursor = {
    val sqlString = "SELECT precursor_list FROM spectrum WHERE id = ?"
    val precursorListAsStr = new SQLiteQuery(mzDbConnection, sqlString).bind(1, spectrumId).extractSingleString()
    ParamTreeParser.parsePrecursor(precursorListAsStr)
  }
}
