package com.github.mzdb4s.io.reader.iterator2

import com.github.mzdb4s.AbstractMzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.msdata.RunSlice
import com.github.sqlite4s._

object LcMsRunSliceIterator {

  private val allRunSlicesSqlQuery  = "SELECT bounding_box.* FROM bounding_box, run_slice " +
  "WHERE run_slice.ms_level = ? AND bounding_box.run_slice_id = run_slice.id  ORDER BY run_slice.begin_mz"

  private val runSlicesSubsetSqlQuery = "SELECT bounding_box.* FROM bounding_box, run_slice " +
  "WHERE run_slice.ms_level = ? " +
  "AND bounding_box.run_slice_id = run_slice.id  " +
  "AND run_slice.end_mz >= ? " +
  "AND run_slice.begin_mz <= ?" +
  "ORDER BY run_slice.begin_mz"
}

class LcMsRunSliceIterator private (
  runSliceHeaderReader: AbstractRunSliceHeaderReader,
  spectrumHeaderReader: AbstractSpectrumHeaderReader,
  dataEncodingReader: AbstractDataEncodingReader,
  sqlQuery: String,
  msLevel: Int,
  stmtBinder: ISQLiteStatement => Unit
)(implicit mzDbCtx: MzDbContext) extends AbstractRunSliceIterator(
  runSliceHeaderReader, spectrumHeaderReader, dataEncodingReader, sqlQuery, msLevel, stmtBinder
) with java.util.Iterator[RunSlice] {

  def this(mzDbReader: AbstractMzDbReader)(implicit mzDbCtx: MzDbContext) {
    // Set msLevel to 1
    this(
      mzDbReader.getRunSliceHeaderReader(),
      mzDbReader.getSpectrumHeaderReader(),
      mzDbReader.getDataEncodingReader(),
      LcMsRunSliceIterator.allRunSlicesSqlQuery,
      msLevel = 1,
      stmtBinder = stmt => stmt.bind(1, 1) // Bind the msLevel
    )
  }

  def this(mzDbReader: AbstractMzDbReader, minRunSliceMz: Double, maxRunSliceMz: Double)(implicit mzDbCtx: MzDbContext) {
    this(
      mzDbReader.getRunSliceHeaderReader(),
      mzDbReader.getSpectrumHeaderReader(),
      mzDbReader.getDataEncodingReader(),
      LcMsRunSliceIterator.runSlicesSubsetSqlQuery,
      msLevel = 1,
      stmtBinder = stmt => {
        stmt.bind(1, 1) // Bind the msLevel
        stmt.bind(2, minRunSliceMz) // Bind the minRunSliceMz
        stmt.bind(3, maxRunSliceMz) // Bind the maxRunSliceMz
      }
    )
  }
}