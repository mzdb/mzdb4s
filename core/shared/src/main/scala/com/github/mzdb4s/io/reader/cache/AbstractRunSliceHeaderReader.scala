package com.github.mzdb4s.io.reader.cache

import scala.collection.mutable.{ArrayBuffer, LongMap}

import com.github.mzdb4s.db.table._
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.MzDbReaderQueries
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.ISQLiteRecordExtraction
import com.github.sqlite4s.query.SQLiteRecord

/*import fr.profi.mzdb.AbstractMzDbReader
import fr.profi.mzdb.db.table.RunSliceTable
import fr.profi.mzdb.model.RunSliceHeader
import fr.profi.mzdb.util.sqlite.ISQLiteRecordExtraction
import fr.profi.mzdb.util.sqlite.SQLiteQuery
import fr.profi.mzdb.util.sqlite.SQLiteRecord*/
// TODO: Auto-generated Javadoc
/**
  * The Class AbstractRunSliceHeaderReader.
  *
  * @author bouyssie
  */
abstract class AbstractRunSliceHeaderReader(
  val bbSizes: BBSizes,
  val entityCache: Option[MzDbEntityCache]
) extends IMzDbEntityCacheContainer {

  private class RunSliceHeaderExtractor extends ISQLiteRecordExtraction[RunSliceHeader] {
    def extractRecord(record: SQLiteRecord): RunSliceHeader = {
      RunSliceHeader(
        record.columnInt(RunSliceTable.ID),
        record.columnInt(RunSliceTable.MS_LEVEL),
        record.columnInt(RunSliceTable.NUMBER),
        record.columnDouble(RunSliceTable.BEGIN_MZ),
        record.columnDouble(RunSliceTable.END_MZ),
        record.columnInt(RunSliceTable.RUN_ID
        )
      )
    }
  }


  /** The _run slice header extractor. */
  private val _runSliceHeaderExtractor: ISQLiteRecordExtraction[RunSliceHeader] = new RunSliceHeaderExtractor()

  protected def getRunSliceHeaders()(implicit mzDbCtx: MzDbContext): Array[RunSliceHeader] = {
    if (this.entityCache.nonEmpty && entityCacheOrNull.runSliceHeaders != null) entityCacheOrNull.runSliceHeaders
    else { // Retrieve the corresponding run slices
      val rsCount = MzDbReaderQueries.getRunSlicesCount()

      val queryStr = "SELECT * FROM run_slice"
      val runSliceHeaders = new Array[RunSliceHeader](rsCount)
      mzDbCtx.newSQLiteQuery(queryStr).extractRecords(_runSliceHeaderExtractor, runSliceHeaders)

      if (this.entityCache.nonEmpty) entityCacheOrNull.runSliceHeaders = runSliceHeaders

      runSliceHeaders
    }
  }

  protected def getRunSliceHeaders(msLevel: Int)(implicit mzDbCtx: MzDbContext): Array[RunSliceHeader] = {

    val runSliceHeaders = if (this.entityCache.nonEmpty && entityCacheOrNull.runSliceHeaders != null)  {
      entityCacheOrNull.runSliceHeaders.filter(_.msLevel == msLevel)
    }
    else {
      val queryStr = "SELECT * FROM run_slice WHERE ms_level=? ORDER BY begin_mz " // number
      val query = mzDbCtx.newSQLiteQuery(queryStr).bind(1, msLevel)
      query.extractRecordList(_runSliceHeaderExtractor).toArray
    }

    runSliceHeaders
  }

  private def _getRunSliceHeaderById(runSliceHeaders: Array[RunSliceHeader]): LongMap[RunSliceHeader] = {
    val runSliceHeaderById = new LongMap[RunSliceHeader](runSliceHeaders.length)

    for (rsh <- runSliceHeaders) {
      runSliceHeaderById.put(rsh.getId, rsh)
    }

    runSliceHeaderById
  }

  def getRunSliceHeaderById(msLevel: Int)(implicit mzDbCtx: MzDbContext): LongMap[RunSliceHeader] = {
    val runSliceHeaders = this.getRunSliceHeaders(msLevel)
    this._getRunSliceHeaderById(runSliceHeaders)
  }

  protected def getRunSliceHeaderById()(implicit mzDbCtx: MzDbContext): LongMap[RunSliceHeader] = {
    if (this.entityCache != null && this.entityCacheOrNull.runSliceHeaderById != null) this.entityCacheOrNull.runSliceHeaderById
    else {
      val runSliceHeaderById = this._getRunSliceHeaderById(this.getRunSliceHeaders())
      if (this.entityCache.nonEmpty) this.entityCacheOrNull.runSliceHeaderById = runSliceHeaderById
      runSliceHeaderById
    }
  }

  protected def getRunSliceHeader(id: Int)(implicit mzDbCtx: MzDbContext): RunSliceHeader = {
    if (this.entityCache.nonEmpty) this.getRunSliceHeaderById().apply(id)
    else {
      val queryStr = "SELECT * FROM run_slice WHERE id = ?"
      mzDbCtx.newSQLiteQuery(queryStr).bind(1, id).extractRecord(this._runSliceHeaderExtractor)
    }
  }

  protected def getRunSliceHeaderForMz(mz: Double, msLevel: Int)(implicit mzDbCtx: MzDbContext): RunSliceHeader = {
    val queryStr = "SELECT * FROM run_slice WHERE ms_level = ? AND begin_mz <= ? AND end_mz > ?"
    mzDbCtx.newSQLiteQuery(queryStr).bind(1, msLevel).bind(2, mz).bind(3, mz).extractRecord(_runSliceHeaderExtractor)
  }

  protected def getRunSliceIdsForMzRange(minMz: Double, maxMz: Double, msLevel: Int)(implicit mzDbCtx: MzDbContext): Array[Int] = {
    val firstRsh = this.getRunSliceHeaderForMz(minMz, msLevel)
    val lastRsh = this.getRunSliceHeaderForMz(maxMz, msLevel)

    val mzHeight = if (msLevel == 1) bbSizes.BB_MZ_HEIGHT_MS1 else bbSizes.BB_MZ_HEIGHT_MSn
    val bufferLength = 1 + ((maxMz - minMz) / mzHeight).toInt

    val queryStr = "SELECT id FROM run_slice WHERE ms_level = ? AND begin_mz >= ? AND end_mz <= ?"

    mzDbCtx.newSQLiteQuery(queryStr)
      .bind(1, msLevel)
      .bind(2, firstRsh.getBeginMz)
      .bind(3, lastRsh.getEndMz)
      .extractInts(bufferLength)
  }
}