package com.github.sqlite4s.query

import java.util

import com.github.sqlite4s.{ISQLiteStatement, SQLiteQuery}

class SQLiteRecordIterator(val query: SQLiteQuery) extends util.Iterator[SQLiteRecord] {
  protected var stmt: ISQLiteStatement = query.getStatement
  protected var nextRecord: SQLiteRecord = _
  protected var _hasNext = false
  protected var _hasNextChecked = false

  def isStatementDisposed: Boolean = if (this.stmt == null || this.stmt.isDisposed) true
  else false

  def dispose(): Unit = if (this.isStatementDisposed == false && this.stmt != null) {
    this.stmt.dispose()
    this.stmt = null
  }

  override def hasNext: Boolean = {
    if (_hasNextChecked == false) {
      this._hasNextChecked = true
      if (stmt.step) this._hasNext = true
      else {
        this.dispose()
        this._hasNext = false
      }
    }
    this._hasNext
  }

  override def next: SQLiteRecord = {
    try if (this.hasNext) {
      this.nextRecord = new SQLiteRecord(this.query)
      this._hasNextChecked = false
      nextRecord
    }
    else new SQLiteRecord(null)
    catch {
      case e: Exception =>
        // this.nextElem = null;
        // don't throw exception => we have a problem with the statement which is
        // closing automatically
        // TODO: find a safe way to check if the statement has been closed
        // rethrow(e);
        new SQLiteRecord(null) // obj;
    }
  }

  override def remove() = throw new UnsupportedOperationException("Unsupported Operation")

  //protected def rethrow(e: Exception) = throw new RuntimeException(e.getMessage)
}