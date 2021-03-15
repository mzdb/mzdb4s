/*package com.github.mzdb4s.io.reader.iterator2

import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.bb2.IBoundingBoxReader
import com.github.mzdb4s.io.reader.cache.AbstractDataEncodingReader
import com.github.mzdb4s.io.reader.cache.AbstractSpectrumHeaderReader
import com.github.sqlite4s._

abstract class AbstractSpectrumSliceIterator protected (val boundingBoxIterator: BoundingBoxIterator) {

  protected val statement: ISQLiteStatement = boundingBoxIterator.statement
  protected var firstBBReader: IBoundingBoxReader = if (boundingBoxIterator.hasNext()) boundingBoxIterator.next() else null

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    sqlQuery: String
  )(implicit mzDbCtx: MzDbContext) = {
    this(
      BoundingBoxIterator(
        spectrumHeaderReader,
        dataEncodingReader,
        sqlQuery
      )
    )
  }

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    sqlQuery: String,
    msLevel: Int,
    stmtBinder: ISQLiteStatement => Unit
  )(implicit mzDbCtx: MzDbContext) = {
    this(
      BoundingBoxIterator(
        spectrumHeaderReader,
        dataEncodingReader,
        sqlQuery,
        msLevel,
        stmtBinder
      )
    )
  }

  def getStatement(): ISQLiteStatement = this.statement

  def closeStatement(): Unit = statement.dispose()

  def hasNext(): Boolean = {
    if (this.firstBBReader != null) { // this.statement.hasRow() ) {//
      true
    }
    else {
      this.closeStatement()
      false
    }
  }

  //def remove(): Unit = throw new UnsupportedOperationException("remove operation is not supported")
}*/