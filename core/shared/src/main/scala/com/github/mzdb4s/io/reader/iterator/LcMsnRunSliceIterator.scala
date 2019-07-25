package com.github.mzdb4s.io.reader.iterator

import com.github.mzdb4s.AbstractMzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.msdata.RunSlice
import com.github.sqlite4s._

/** Class used for DIA/SWATH data **/
object LcMsnRunSliceIterator {

  /*
  private val sameIsolationWindowRunSlicesSqlQuery = "SELECT bounding_box.* FROM bounding_box, bounding_box_msn_rtree, run_slice " +
    "WHERE bounding_box_msn_rtree.id = bounding_box.id " +
    "AND bounding_box.run_slice_id = run_slice.id " +
    "AND run_slice.ms_level = ? " +
    "AND bounding_box_msn_rtree.max_parent_mz >= ? " +
    "AND bounding_box_msn_rtree.min_parent_mz <= ? " + "ORDER BY run_slice.begin_mz"
  */

  private val sameIsolationWindowRunSlicesSqlQuery = "SELECT bounding_box.* FROM bounding_box, bounding_box_msn_rtree " +
    "WHERE bounding_box_msn_rtree.id = bounding_box.id " +
    "AND bounding_box_msn_rtree.max_parent_mz >= ? " +
    "AND bounding_box_msn_rtree.min_parent_mz <= ? ORDER BY bounding_box_msn_rtree.min_mz"

  private val sameIsolationWindowRunSlicesSubsetSqlQuery = "SELECT bounding_box.* FROM bounding_box, bounding_box_msn_rtree, run_slice " +
    "WHERE bounding_box_msn_rtree.id = bounding_box.id " +
    "AND bounding_box.run_slice_id = run_slice.id " +
    "AND run_slice.ms_level = ? " +
    "AND run_slice.end_mz >= ? " +
    "AND run_slice.begin_mz <= ? " +
    "AND bounding_box_msn_rtree.max_parent_mz >= ? " +
    "AND bounding_box_msn_rtree.min_parent_mz <= ? " +
    "ORDER BY run_slice.begin_mz"
}

class LcMsnRunSliceIterator private (
  runSliceHeaderReader: AbstractRunSliceHeaderReader,
  spectrumHeaderReader: AbstractSpectrumHeaderReader,
  dataEncodingReader: AbstractDataEncodingReader,
  sqlQuery: String,
  msLevel: Int,
  stmtBinder: ISQLiteStatement => Unit
)(implicit mzDbCtx: MzDbContext) extends AbstractRunSliceIterator(
  runSliceHeaderReader, spectrumHeaderReader, dataEncodingReader, sqlQuery, msLevel, stmtBinder
) with java.util.Iterator[RunSlice] {

  def this(mzDbReader: AbstractMzDbReader, minParentMz: Double, maxParentMz: Double)(implicit mzDbCtx: MzDbContext) {
    // Set msLevel to 1
    this(
      mzDbReader.getRunSliceHeaderReader(),
      mzDbReader.getSpectrumHeaderReader(),
      mzDbReader.getDataEncodingReader(),
      LcMsnRunSliceIterator.sameIsolationWindowRunSlicesSqlQuery,
      msLevel = 2,
      stmtBinder = stmt => {
        //stmt.bind(1, 2) // Bind the msLevel
        stmt.bind(1, minParentMz) // Bind the minParentMz
        stmt.bind(2, maxParentMz) // Bind the maxParentMz
      }
    )
  }

  def this(mzDbReader: AbstractMzDbReader, minRunSliceMz: Double, maxRunSliceMz: Double, minParentMz: Double, maxParentMz: Double)(implicit mzDbCtx: MzDbContext){
    this(
      mzDbReader.getRunSliceHeaderReader(),
      mzDbReader.getSpectrumHeaderReader(),
      mzDbReader.getDataEncodingReader(),
      LcMsnRunSliceIterator.sameIsolationWindowRunSlicesSubsetSqlQuery,
      msLevel = 2,
      // FIXME: what about msLevel > 2 ?
      stmtBinder = stmt => {
        stmt.bind(1, 2) // Bind the msLevel
        stmt.bind(2, minParentMz) // Bind the minParentMz
        stmt.bind(3, maxParentMz) // Bind the maxParentMz
        stmt.bind(4, minRunSliceMz) // Bind the minRunSliceMz
        stmt.bind(5, maxRunSliceMz) // Bind the maxRunSliceMz
      }
    )
  }
}