package com.github.mzdb4s.io.reader

import com.github.mzdb4s.io.MzDbContext
import com.github.sqlite4s.SQLiteQuery

object MzDbReaderQueries {

  def getModelVersion()(implicit mzDbCtx: MzDbContext): String = {
    mzDbCtx.newSQLiteQuery("SELECT version FROM mzdb LIMIT 1").extractSingleString()
  }

  def getPwizMzDbVersion()(implicit mzDbCtx: MzDbContext): String = {
    mzDbCtx.newSQLiteQuery("SELECT version FROM software WHERE name LIKE '%mzDB'").extractSingleString()
  }

  /**
    * Gets the last time.
    *
    * @return float the rt of the last spectrum
    */
  def getLastTime()(implicit mzDbCtx: MzDbContext): Float = { // Retrieve the number of spectra
    mzDbCtx.newSQLiteQuery("SELECT time FROM spectrum ORDER BY id DESC LIMIT 1").extractSingleDouble().toFloat
  }

  /**
    * Gets the max ms level.
    *
    * @return the max ms level
    */
  def getMaxMsLevel()(implicit mzDbCtx: MzDbContext): Int = {
    mzDbCtx.newSQLiteQuery("SELECT max(ms_level) FROM run_slice").extractSingleInt()
  }

  /**
    * Gets the mz range.
    *
    * @param msLevel the ms level
    * @return runSlice min mz and runSlice max mz
    */
  def getMzRange(msLevel: Int)(implicit mzDbCtx: MzDbContext): Array[Int] = {
    val stmt = mzDbCtx.mzDbConnection.prepare("SELECT min(begin_mz), max(end_mz) FROM run_slice WHERE ms_level=?")
    stmt.bind(1, msLevel)
    stmt.step
    val minMz = stmt.columnInt(0)
    val maxMz = stmt.columnInt(1)
    stmt.dispose()
    val mzRange = Array(minMz, maxMz)
    mzRange
  }

  /**
    * Gets the bounding box count.
    *
    * @return int, the number of bounding box
    */
  def getBoundingBoxesCount()(implicit mzDbCtx: MzDbContext): Int = getTableRecordsCount("bounding_box")

  /**
    * Gets the bounding box count.
    *
    * @param runSliceId the run slice id
    * @return the number of bounding box contained in the specified runSliceId
    */
  def getBoundingBoxesCount(runSliceId: Int)(implicit mzDbCtx: MzDbContext): Int = {
    val sqlString = "SELECT count(*) FROM bounding_box WHERE bounding_box.run_slice_id = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, runSliceId).extractSingleInt()
  }

  /**
    * Gets the cycle count.
    *
    * @return the cycle count
    */
  def getCyclesCount()(implicit mzDbCtx: MzDbContext): Int = {
    val sqlString = "SELECT max(cycle) FROM spectrum"
    mzDbCtx.newSQLiteQuery(sqlString).extractSingleInt()
  }

  /**
    * Gets the data encoding count.
    *
    * @return the data encoding count
    */
  def getDataEncodingsCount()(implicit mzDbCtx: MzDbContext): Int = getTableRecordsCount("data_encoding")

  /**
    * Gets the spectra count.
    *
    * @return int the number of spectra
    */
  def getSpectraCount()(implicit mzDbCtx: MzDbContext): Int = getTableRecordsCount("spectrum")

  /**
    * Gets the spectra count for a given MS level.
    *
    * @return int the number of spectra
    */
  def getSpectraCount(msLevel: Int)(implicit mzDbCtx: MzDbContext): Int = {
    val sqlString = "SELECT count(*) FROM spectrum WHERE ms_level = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, msLevel).extractSingleInt()
  }

  /**
    * Gets the run slice count.
    *
    * @return int the number of runSlice
    */
  def getRunSlicesCount()(implicit mzDbCtx: MzDbContext): Int = getTableRecordsCount("run_slice")

  /**
    * Gets the table records count.
    *
    * @param tableName the table name
    * @return the int
    */
  def getTableRecordsCount(tableName: String)(implicit mzDbCtx: MzDbContext): Int = {
    mzDbCtx.newSQLiteQuery("SELECT seq FROM sqlite_sequence WHERE name = ?").bind(1, tableName).extractSingleInt()
  }

  /**
    * Gets the bounding box data.
    *
    * @param bbId the bb id
    * @return the bounding box data
    */
  def getBoundingBoxData(bbId: Int)(implicit mzDbCtx: MzDbContext): Array[Byte] = {
    val sqlString = "SELECT data FROM bounding_box WHERE bounding_box.id = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, bbId).extractSingleBlob()
  }

  /**
    * Gets the bounding box first spectrum index.
    *
    * @param spectrumId the spectrum id
    * @return the bounding box first spectrum index
    */
  def getBoundingBoxFirstSpectrumId(spectrumId: Long)(implicit mzDbCtx: MzDbContext): Long = {
    val sqlString = "SELECT bb_first_spectrum_id FROM spectrum WHERE id = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, spectrumId).extractSingleLong()
  }

  /**
    * Gets the bounding box min mz.
    *
    * @param bbId
    * the bb id
    * @return the bounding box min mz
    */
  def getBoundingBoxMinMz(bbId: Int)(implicit mzDbCtx: MzDbContext): Float = {
    val sqlString = "SELECT min_mz FROM bounding_box_rtree WHERE bounding_box_rtree.id = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, bbId).extractSingleDouble().toFloat
  }

  /**
    * Gets the bounding box min time.
    *
    * @param bbId the bb id
    * @return the bounding box min time
    */
  def getBoundingBoxMinTime(bbId: Int)(implicit mzDbCtx: MzDbContext): Float = {
    val sqlString = "SELECT min_time FROM bounding_box_rtree WHERE bounding_box_rtree.id = ?"
    mzDbCtx.newSQLiteQuery(sqlString).bind(1, bbId).extractSingleDouble().toFloat
  }

  /**
    * Gets the bounding box ms level.
    *
    * @param bbId the bb id
    * @return the bounding box ms level
    */
  def getBoundingBoxMsLevel(bbId: Int)(implicit mzDbCtx: MzDbContext): Int = { // FIXME: check that the mzDB file has the bounding_box_msn_rtree table
    val sqlString1 = "SELECT run_slice_id FROM bounding_box WHERE id = ?"
    val runSliceId = mzDbCtx.newSQLiteQuery(sqlString1).bind(1, bbId).extractSingleInt()
    val sqlString2 = "SELECT ms_level FROM run_slice WHERE run_slice.id = ?"
    mzDbCtx.newSQLiteQuery(sqlString2).bind(1, runSliceId).extractSingleInt()
    /*
         * String sqlString =
         * "SELECT min_ms_level FROM bounding_box_msn_rtree WHERE bounding_box_msn_rtree.id = ?"; return new
         * SQLiteQuery(connection, sqlString).bind(1, bbId).extractSingleInt();
         */
  }
}