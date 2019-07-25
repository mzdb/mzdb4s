package com.github.mzdb4s.io.reader.iterator

import com.github.mzdb4s.io.MzDbContext

import scala.collection.mutable.LongMap
import com.github.mzdb4s.io.reader.bb.BoundingBoxBuilder
import com.github.mzdb4s.io.reader.cache.AbstractDataEncodingReader
import com.github.mzdb4s.io.reader.cache.AbstractSpectrumHeaderReader
import com.github.mzdb4s.msdata.BoundingBox
import com.github.mzdb4s.msdata.DataEncoding
import com.github.mzdb4s.msdata.SpectrumHeader
import com.github.sqlite4s._

object BoundingBoxIterator {

  def apply(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    sqlQuery: String
  )(implicit mzDbCtx: MzDbContext): BoundingBoxIterator = {
    new BoundingBoxIterator(
      spectrumHeaderReader,
      dataEncodingReader,
      // Create a new statement (will be automatically closed by the StatementIterator)
      mzDbCtx.mzDbConnection.prepare(sqlQuery, true) // true = cached enabled)
    )
  }

  def apply(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    sqlQuery: String,
    msLevel: Int,
    stmtBinder: ISQLiteStatement => Unit
  )(implicit mzDbCtx: MzDbContext): BoundingBoxIterator = {
    new BoundingBoxIterator(
      spectrumHeaderReader,
      dataEncodingReader,
      // Create a new statement (will be automatically closed by the StatementIterator)
      {
        val stmt = mzDbCtx.mzDbConnection.prepare(sqlQuery, true)  // true = cached enabled)
        // Call the statement binder
        stmtBinder(stmt)
        stmt
      }
    )
  }
}

class BoundingBoxIterator protected(override val statement: ISQLiteStatement) extends AbstractStatementIterator[BoundingBox](statement) {

  final protected var spectrumHeaderById: LongMap[SpectrumHeader] = _
  final protected var dataEncodingBySpectrumId: LongMap[DataEncoding] = _

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    stmt: ISQLiteStatement
  )(implicit mzDbCtx: MzDbContext) {
    this(stmt)
    this.spectrumHeaderById = spectrumHeaderReader.getSpectrumHeaderById()(mzDbCtx)
    this.dataEncodingBySpectrumId = dataEncodingReader.getDataEncodingBySpectrumId()(mzDbCtx)
  }

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    stmt: ISQLiteStatement,
    msLevel: Int
  )(implicit mzDbCtx: MzDbContext) {
    this(stmt)
    if (msLevel == 1) this.spectrumHeaderById = spectrumHeaderReader.getMs1SpectrumHeaderById()(mzDbCtx)
    else if (msLevel == 2) this.spectrumHeaderById = spectrumHeaderReader.getMs2SpectrumHeaderById()(mzDbCtx)
    else if (msLevel == 3) this.spectrumHeaderById = spectrumHeaderReader.getMs3SpectrumHeaderById()(mzDbCtx)
    else throw new IllegalArgumentException("unsupported MS level: " + msLevel)
    this.dataEncodingBySpectrumId = dataEncodingReader.getDataEncodingBySpectrumId()(mzDbCtx)
  }

  override def extractObject(stmt: ISQLiteStatement): BoundingBox = {
    val bbId = stmt.columnInt(0)
    val bbBytes = stmt.columnBlob(1)
    val runSliceId = stmt.columnInt(2)
    val firstSpectrumId = stmt.columnInt(3)
    val lastSpectrumId = stmt.columnInt(4)

    val bb = BoundingBoxBuilder.buildBB(
      bbId, bbBytes, firstSpectrumId, lastSpectrumId, this.spectrumHeaderById, this.dataEncodingBySpectrumId
    )
    bb.setRunSliceId(runSliceId)

    bb
  }
}