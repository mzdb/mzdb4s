/*package com.github.mzdb4s.io.reader.iterator2

import com.github.mzdb4s.io.MzDbContext

import scala.collection.mutable.LongMap
import com.github.mzdb4s.io.reader.bb2._
import com.github.mzdb4s.io.reader.cache.AbstractDataEncodingReader
import com.github.mzdb4s.io.reader.cache.AbstractSpectrumHeaderReader
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

class BoundingBoxIterator protected(override val statement: ISQLiteStatement)(implicit mzDbCtx: MzDbContext) extends AbstractStatementIterator[IBoundingBoxReader](statement) {

  final private implicit val _bbIdxFactory: BoundingBoxIndexFactory = new BoundingBoxIndexFactory()
  _bbIdxFactory.acquirePeaksBuffer(2048) // acquire a first large buffer to avoid creation of small buffers

  final private var _currentBlob = mzDbCtx.mzDbConnection.blob("bounding_box","data", 1, false)

  final protected var _spectrumHeaderById: LongMap[SpectrumHeader] = _
  final protected var _dataEncodingBySpectrumId: LongMap[DataEncoding] = _

  def spectrumHeaderById: LongMap[SpectrumHeader] = _spectrumHeaderById
  def dataEncodingBySpectrumId: LongMap[DataEncoding] = _dataEncodingBySpectrumId

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    stmt: ISQLiteStatement
  )(implicit mzDbCtx: MzDbContext) {
    this(stmt)
    this._spectrumHeaderById = spectrumHeaderReader.getSpectrumHeaderById()(mzDbCtx)
    this._dataEncodingBySpectrumId = dataEncodingReader.getDataEncodingBySpectrumId()(mzDbCtx)
  }

  def this(
    spectrumHeaderReader: AbstractSpectrumHeaderReader,
    dataEncodingReader: AbstractDataEncodingReader,
    stmt: ISQLiteStatement,
    msLevel: Int
  )(implicit mzDbCtx: MzDbContext) {
    this(stmt)
    if (msLevel == 1) this._spectrumHeaderById = spectrumHeaderReader.getMs1SpectrumHeaderById()(mzDbCtx)
    else if (msLevel == 2) this._spectrumHeaderById = spectrumHeaderReader.getMs2SpectrumHeaderById()(mzDbCtx)
    else if (msLevel == 3) this._spectrumHeaderById = spectrumHeaderReader.getMs3SpectrumHeaderById()(mzDbCtx)
    else throw new IllegalArgumentException("unsupported MS level: " + msLevel)
    this._dataEncodingBySpectrumId = dataEncodingReader.getDataEncodingBySpectrumId()(mzDbCtx)
  }

  override def closeStatement(): Unit = {
    //_currentBlob.dispose()
    super.closeStatement()
  }

  override def next(): IBoundingBoxReader = {
    // Dispose previous IBoundingBoxReader
    if (this.nextElem != null)
      this.nextElem.dispose()

    super.next()
  }

  override def extractObject(stmt: ISQLiteStatement): IBoundingBoxReader = {
    val bbId = stmt.columnInt(0)
    val bbBytes = stmt.columnBlob(1)
    val runSliceId = stmt.columnInt(2)
    val firstSpectrumId = stmt.columnLong(3)
    val lastSpectrumId = stmt.columnLong(4)

    // FIXME: this is a workaround for an sqlite4java bug ("SQL logic error or missing database")
    // The second issue is that creating a new blob from scratch uses more memory
    /*val bbReader = if (! _currentBlob.reopen(bbId)) {
      //_currentBlob.dispose()
      //_currentBlob = mzDbCtx.mzDbConnection.blob("bounding_box","data", bbId, false)

      BoundingBoxReader(
        bbId,
        stmt.columnBlob(1),
        firstSpectrumId,
        lastSpectrumId,
        this._spectrumHeaderById,
        this._dataEncodingBySpectrumId
      )
    } else {
      val blobSize = _currentBlob.getSize()
      val peaksBuffer = new Array[Byte](blobSize) //_bbIdxFactory.acquirePeaksBuffer(blobSize)
      _currentBlob.read(0, peaksBuffer, 0, blobSize)

      BoundingBoxReader(
        bbId,
        peaksBuffer, //stmt.columnBlob(1), //_currentBlob,
        firstSpectrumId,
        lastSpectrumId,
        this._spectrumHeaderById,
        this._dataEncodingBySpectrumId
      )
    }*/

    val bbReader = BoundingBoxReader(
      bbId,
      bbBytes,
      runSliceId,
      firstSpectrumId,
      lastSpectrumId,
      this._spectrumHeaderById,
      this._dataEncodingBySpectrumId
    )

    bbReader
  }
}*/