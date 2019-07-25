package com.github.mzdb4s.io.reader.iterator

import scala.collection.mutable

import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.ISQLiteStatement

abstract class AbstractRunSliceIterator protected(
  runSliceHeaderReader: AbstractRunSliceHeaderReader,
  spectrumHeaderReader: AbstractSpectrumHeaderReader,
  dataEncodingReader: AbstractDataEncodingReader,
  sqlQuery: String,
  msLevel: Int,
  stmtBinder: ISQLiteStatement => Unit
)(implicit mzDbCtx: MzDbContext) extends AbstractSpectrumSliceIterator(
  spectrumHeaderReader, dataEncodingReader, sqlQuery, msLevel, stmtBinder
) with java.util.Iterator[RunSlice] {

  val runSliceHeaderById = runSliceHeaderReader.getRunSliceHeaderById(msLevel)

  protected var spectrumSliceBuffer: Array[SpectrumSlice] = _
  protected var bbHasNext = true

  protected def initSpectrumSliceBuffer(): Unit = {
    this.spectrumSliceBuffer = this.firstBB.toSpectrumSlices()

    val slBuffer = spectrumSliceBuffer.toBuffer

    var sameRunSlice = true
    while ( {bbHasNext = boundingBoxIterator.hasNext(); bbHasNext} && sameRunSlice ) {
      val bb = boundingBoxIterator.next()
      var sameRunSlice = bb.getRunSliceId == this.firstBB.getRunSliceId
      if (sameRunSlice) slBuffer ++= bb.toSpectrumSlices()
      else {
        this.firstBB = bb
      }
    }

    this.spectrumSliceBuffer = slBuffer.toArray

    if (!bbHasNext) this.firstBB = null
  }

  override def next(): RunSlice = {
    initSpectrumSliceBuffer()

    val runSliceId = this.spectrumSliceBuffer(0).getRunSliceId()
    val rsd = RunSliceData(runSliceId, this.spectrumSliceBuffer)
    // rsd.buildPeakListBySpectrumId();
    val rsh = this.runSliceHeaderById(runSliceId)

    RunSlice(rsh, rsd)
  }
}