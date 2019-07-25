package com.github.mzdb4s.io.reader.iterator

import java.util

import com.github.sqlite4s.ISQLiteStatement

abstract class AbstractStatementIterator[E] (val statement: ISQLiteStatement) extends util.Iterator[E] { // with IStatementExtractor[E]

  protected var isStatementClosed = false
  protected var nextElem: E = _

  def extractObject(stmt: ISQLiteStatement): E

  def closeStatement(): Unit = {
    statement.dispose()
    // if (! statement.isDisposed() ) {//!isStatementClosed) {
    // if (statement != null) {
    // isStatementClosed = true;
    // }
  }

  override def hasNext(): Boolean = {
    if (!statement.isDisposed() && statement.step()) true
    else {
      this.closeStatement()
      false
    }
  }

  override def next(): E = {
    this.nextElem = this.extractObject(statement)
    nextElem
  }

  //override def remove() = throw new UnsupportedOperationException("remove operation is not supported")
}