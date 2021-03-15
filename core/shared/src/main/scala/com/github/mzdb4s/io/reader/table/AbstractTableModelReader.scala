package com.github.mzdb4s.io.reader.table

import scala.collection.Seq

import com.github.mzdb4s.io.MzDbContext
import com.github.sqlite4s.ISQLiteRecordExtraction
import com.github.sqlite4s.query.SQLiteRecord

abstract class AbstractTableModelReader[T] extends ISQLiteRecordExtraction[T] {
  implicit val mzDbContext: MzDbContext

  def extractRecord(record: SQLiteRecord): T

  protected def getRecord(tableName: String, id: Int): T = mzDbContext.newSQLiteQuery(
    s"SELECT * FROM $tableName WHERE id = ?"
  ).bind(1, id).extractRecord(this)

  protected def getRecordList(tableName: String): Seq[T] = mzDbContext.newSQLiteQuery(
    s"SELECT * FROM $tableName"
  ).extractRecordList(this)
}