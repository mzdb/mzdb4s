package com.github.sqlite4s.query

import java.io.InputStream

import com.github.sqlite4s.{ISQLiteStatement, SQLiteQuery}

/**
  * @author David Bouyssie
  *
  */
class SQLiteRecord(val sqliteQuery: SQLiteQuery) extends AnyVal {
  private def stmt: ISQLiteStatement = sqliteQuery.getStatement

  def getStatement: ISQLiteStatement = this.stmt

  // TODO: replace calls to name().toLowerCase() by toString() ?
  def columnString(column: Enumeration#Value): String = this.columnString(column.toString)

  def columnString(columnName: String): String = this.stmt.columnString(sqliteQuery.getColumnIndex(columnName))

  def columnInt(column: Enumeration#Value): Int = this.columnInt(column.toString)

  def columnInt(columnName: String): Int = this.stmt.columnInt(sqliteQuery.getColumnIndex(columnName))

  def columnDouble(column: Enumeration#Value): Double = this.columnDouble(column.toString)

  def columnDouble(columnName: String): Double = this.stmt.columnDouble(sqliteQuery.getColumnIndex(columnName))

  def columnLong(column: Enumeration#Value): Long = this.columnLong(column.toString)

  def columnLong(columnName: String): Long = this.stmt.columnLong(sqliteQuery.getColumnIndex(columnName))

  def columnBlob(column: Enumeration#Value): Array[Byte] = this.columnBlob(column.toString)

  def columnBlob(columnName: String): Array[Byte] = this.stmt.columnBlob(sqliteQuery.getColumnIndex(columnName))

  //def columnStream(column: Enumeration#Value): InputStream = this.columnStream(column.toString)

  //def columnStream(columnName: String): InputStream = this.stmt.columnStream(sqliteQuery.getColumnIndex(columnName))

  def columnNull(column: Enumeration#Value): Boolean = this.columnNull(column.toString)

  def columnNull(columnName: String): Boolean = this.stmt.columnNull(sqliteQuery.getColumnIndex(columnName))
}