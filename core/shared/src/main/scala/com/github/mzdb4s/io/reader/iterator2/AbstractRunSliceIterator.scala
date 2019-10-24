package com.github.mzdb4s.io.reader.iterator2

import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.cache._
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder._
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

  implicit protected val spectrumDataBuilderFactory = new SpectrumDataBuilderFactory()

  // TODO: how to determine the best initial buffer size???
  protected var spectrumSliceBuffer: ArrayBuffer[SpectrumSlice] = new ArrayBuffer[SpectrumSlice](100)
  protected var bbHasNext = true

  protected def initSpectrumSliceBuffer(): Unit = {
    spectrumSliceBuffer.clear()

    this.firstBBReader.readAllSpectrumSlices(spectrumSliceBuffer)

    var sameRunSlice = true
    while ( sameRunSlice && {bbHasNext = boundingBoxIterator.hasNext(); bbHasNext} ) {
      val bbReader = boundingBoxIterator.next()
      var sameRunSlice = bbReader.runSliceId == this.firstBBReader.runSliceId
      if (sameRunSlice) bbReader.readAllSpectrumSlices(spectrumSliceBuffer)
      else {
        this.firstBBReader = bbReader
      }
    }

    if (!bbHasNext) this.firstBBReader = null
  }

  override def next(): RunSlice = {
    initSpectrumSliceBuffer()

    val runSliceId = this.spectrumSliceBuffer.head.getRunSliceId()
    val rsh = this.runSliceHeaderById(runSliceId)
    val rsd = RunSliceData(runSliceId, this.spectrumSliceBuffer)

    RunSlice(rsh, rsd)
  }
}