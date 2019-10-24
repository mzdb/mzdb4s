package com.github.mzdb4s.io.reader.iterator

import java.util.Comparator
import java.util.PriorityQueue

import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.AbstractMzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder.SpectrumDataBuilder

//import static fr.profi.mzdb.utils.lambda.JavaStreamExceptionWrappers.rethrowConsumer;
object SpectrumIterator {
  //private val allMsLevelsSqlQuery = "SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id"
  //private val singleMsLevelSqlQuery = allMsLevelsSqlQuery + " AND spectrum.ms_level= ?"
  // FIXME: find the most appropriate query
  private val allMsLevelsSqlQuery = "SELECT * FROM bounding_box"
  private val singleMsLevelSqlQuery = "SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id AND spectrum.ms_level= ?"
  private val PRIORITY_QUEUE_INITIAL_CAPACITY = 200
}

class SpectrumIterator protected (
  boundingBoxIterator: BoundingBoxIterator
)(implicit mzDbCtx: MzDbContext) extends AbstractSpectrumSliceIterator(boundingBoxIterator) with java.util.Iterator[Spectrum] {

  // TODO: generalize this iteratorConsumed to other iterators
  protected var iteratorConsumed = false

  protected var bbHasNext = true

  protected var firstSpectrumSlices: Array[SpectrumSlice] = _
  protected var spectrumSliceBuffer: ArrayBuffer[Array[SpectrumSlice]] = new ArrayBuffer[Array[SpectrumSlice]]

  private var spectrumBuffer: ArrayBuffer[Spectrum] = new ArrayBuffer[Spectrum] // used if usePriorityQueue is false
  private var spectrumIdx = 0

  private var usePriorityQueue = false
  private var priorityQueue: PriorityQueue[Spectrum] = _ // used if usePriorityQueue is true

  def this(mzDbReader: AbstractMzDbReader)(implicit mzDbCtx: MzDbContext) {
    this(
      BoundingBoxIterator(
        mzDbReader.getSpectrumHeaderReader(),
        mzDbReader.getDataEncodingReader(),
        SpectrumIterator.allMsLevelsSqlQuery
      )(mzDbCtx)
    )

    this.usePriorityQueue = true
    this.priorityQueue = new PriorityQueue[Spectrum](
      SpectrumIterator.PRIORITY_QUEUE_INITIAL_CAPACITY,
      new Comparator[Spectrum]() {
        override def compare(s1: Spectrum, s2: Spectrum): Int = {
          (s1.getHeader.getId - s2.getHeader.getId).toInt
        }
      }
    )

    this.initSpectrumSliceBuffer()
  }

  // TODO: implement the SpectrumRangeIterator in an additional constructor
  def this(mzDbReader: AbstractMzDbReader, msLevel: Int)(implicit mzDbCtx: MzDbContext) {
    this(
      BoundingBoxIterator(
        mzDbReader.getSpectrumHeaderReader(),
        mzDbReader.getDataEncodingReader(),
        SpectrumIterator.singleMsLevelSqlQuery,
        msLevel,
        stmt => stmt.bind(1, msLevel) // Bind the msLevel
      )(mzDbCtx)
    )

    this.usePriorityQueue = false
    this.priorityQueue = null

    this.initSpectrumSliceBuffer()
  }

  protected def initSpectrumSliceBuffer(): Unit = {
    spectrumSliceBuffer.clear()

    if (this.firstSpectrumSlices != null)
      this.spectrumSliceBuffer += this.firstSpectrumSlices
    else
      this.spectrumSliceBuffer += this.firstBB.toSpectrumSlices()

    this.spectrumIdx = 0

    var sameSpectrum = true
    var continueSlicesLoading = false

    val curSH = spectrumSliceBuffer.head.head.getHeader

    // Build spectrum slice buffer
    while ( sameSpectrum && {bbHasNext = boundingBoxIterator.hasNext(); bbHasNext}) {
      val bb = boundingBoxIterator.next()
      val sSlices = bb.toSpectrumSlices()

      if (sSlices != null) { // FIXME: it should not happen with current spec => make an assertion?
        val nextSH = sSlices.head.getHeader

        if (nextSH.getSpectrumId == curSH.getSpectrumId) {
          spectrumSliceBuffer += sSlices
        }
        else {
          sameSpectrum = false

          // Keep this bounding box for next iteration
          this.firstBB = bb
          this.firstSpectrumSlices = sSlices

          if (usePriorityQueue) { // Put the loaded spectra in the priority queue
            val nextMsLevel = nextSH.getMsLevel
            // Check if we need to continue loading the spectrum slices
            if (curSH.getCycle == nextSH.getCycle || curSH.getMsLevel == nextMsLevel || nextMsLevel > 1 || nextSH.id < curSH.id)
              continueSlicesLoading = true
          }
        }
      }
    }

    if (!usePriorityQueue) {
      spectrumBuffer.clear()
      // TODO: optimize transposition
      spectrumBuffer = spectrumSliceBuffer.transpose.map(_spectrumSlicesToSpectrum)
    } else {
      // Put the loaded spectra in the priority queue
      // TODO: optimize transposition
      spectrumSliceBuffer.transpose.foreach { spectrumSlices =>
        priorityQueue.add(_spectrumSlicesToSpectrum(spectrumSlices))
      }

      if (continueSlicesLoading) this.initSpectrumSliceBuffer()
    }

  }

  private def _spectrumSlicesToSpectrum(spectrumSlices: ArrayBuffer[SpectrumSlice]): Spectrum = {
    var peaksCount = 0
    val spectrumDataList = spectrumSlices.map { ss =>
      val ssData = ss.getData
      peaksCount += ssData.peaksCount
      ssData
    }
    Spectrum(spectrumSlices.head.header,SpectrumDataBuilder.mergeSpectrumDataList(spectrumDataList, peaksCount))
  }

  override def next(): Spectrum = { // firstSpectrumSlices is not null
    if (this.iteratorConsumed)
      throw new java.util.NoSuchElementException("no more spectrum available")

    var spectrum: Spectrum = null
    var noMoreSpectrum = false

    if (usePriorityQueue) {
      spectrum = priorityQueue.poll
      noMoreSpectrum = priorityQueue.isEmpty
    }
    else {
      val spectrumIdxCopy = spectrumIdx
      spectrumIdx += 1

      spectrum = spectrumBuffer(spectrumIdxCopy)
      noMoreSpectrum = spectrumIdx == spectrumBuffer.length
    }

    // If no more spectrum slices
    if (noMoreSpectrum) {
      if (bbHasNext) initSpectrumSliceBuffer()
      else {
        this.firstBB = null
        this.iteratorConsumed = true
        this.spectrumSliceBuffer.clear()
        this.spectrumBuffer.clear()
      }
    }

    spectrum
  }
}