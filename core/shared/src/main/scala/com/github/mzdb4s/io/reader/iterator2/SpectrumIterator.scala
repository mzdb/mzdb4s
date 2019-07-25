package com.github.mzdb4s.io.reader.iterator2

import java.util.Comparator
import java.util.PriorityQueue

import scala.collection.mutable.ArrayBuffer
import com.github.mzdb4s.AbstractMzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.msdata.builder._

//import static fr.profi.mzdb.utils.lambda.JavaStreamExceptionWrappers.rethrowConsumer;
object SpectrumIterator {
  //private val allMsLevelsSqlQuery = "SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id"
  //private val singleMsLevelSqlQuery = allMsLevelsSqlQuery + " AND spectrum.ms_level= ?"
  private val allMsLevelsSqlQuery = "SELECT * FROM bounding_box"
  private val singleMsLevelSqlQuery = "SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id AND spectrum.ms_level= ?"
  private val PRIORITY_QUEUE_INITIAL_CAPACITY = 200
}

class SpectrumIterator protected (
  boundingBoxIterator: BoundingBoxIterator
)(implicit mzDbCtx: MzDbContext) extends AbstractSpectrumSliceIterator(boundingBoxIterator) with java.util.Iterator[Spectrum] {

  // TODO: provide this???
  protected val spectrumDataBuilderFactory = new SpectrumDataBuilderFactory()

  // TODO: generalize this iteratorConsumed to other iterators
  protected var iteratorConsumed = false

  protected var bbHasNext = true

  //protected var firstSpectrumSlices: Array[SpectrumSlice] = _
  //protected var spectrumSliceBuffer: ArrayBuffer[Array[SpectrumSlice]] = new ArrayBuffer[Array[SpectrumSlice]]

  private var spectrumBuilderBuffer: ArrayBuffer[SpectrumBuilder] = new ArrayBuffer[SpectrumBuilder]
  private var spectrumBuildersCache: ArrayBuffer[SpectrumBuilder] = new ArrayBuffer[SpectrumBuilder]

  private var spectrumBuffer: ArrayBuffer[Spectrum] = new ArrayBuffer[Spectrum] // used if usePriorityQueue is false
  private var spectrumBufferReadIdx = 0

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
    /*if (this.firstSpectrumSlices != null)
      this.spectrumSliceBuffer += this.firstSpectrumSlices
    else
      this.spectrumSliceBuffer += this.firstBB.toSpectrumSlices()*/

    // All previous spectra have consumed => let's recycle the buffer
    /*spectrumBuilderBuffer.foreach { sb =>
      spectrumDataBuilderFactory.releaseBuilder(sb.spectrumDataBuilder)
    }*/
    spectrumBuilderBuffer.clear()

    val spectrumHeaderById = boundingBoxIterator.spectrumHeaderById
    val spectraCount = this.firstBBReader.getSpectraCount()

    var i = 0
    while (i < spectraCount) {
      val spectrumId = firstBBReader.getSpectrumIdAt(i)
      val spectrumHeader = spectrumHeaderById(spectrumId)
      val sdb = spectrumDataBuilderFactory.acquireBuilder(spectrumHeader.peaksCount)
      spectrumBuilderBuffer += new SpectrumBuilder(spectrumHeader, sdb)
      i += 1
    }
    this.firstBBReader.readAllSpectrumSlices(this.spectrumBuilderBuffer)

    this.spectrumBufferReadIdx = 0
    var sameSpectrum = true
    var continueSlicesLoading = false

    val curSpecBuilder = spectrumBuilderBuffer.head
    val curSH = curSpecBuilder.header
    val curBBFirstSpectrumId = curSH.id

    // Build spectrum slice buffer
    while ( sameSpectrum && {bbHasNext = boundingBoxIterator.hasNext(); bbHasNext}) {
      val nextBBReader = boundingBoxIterator.next()
      val nextBBFirstSpectrumId = nextBBReader.firstSpectrumId

      if (nextBBFirstSpectrumId == curBBFirstSpectrumId) {
        nextBBReader.readAllSpectrumSlices(this.spectrumBuilderBuffer)
      }
      else {
        sameSpectrum = false

        // Keep this bounding box for next iteration
        this.firstBBReader = nextBBReader

        if (usePriorityQueue) { // Put the loaded spectra in the priority queue
          val nextSH = spectrumHeaderById(nextBBFirstSpectrumId)
          val nextMsLevel = nextSH.getMsLevel
          // Check if we need to continue loading the spectrum slices
          if (curSH.getCycle == nextSH.getCycle || curSH.getMsLevel == nextMsLevel || nextMsLevel > 1)
            continueSlicesLoading = true
        }
      }
    }

    if (!usePriorityQueue) {
      spectrumBuffer.clear()
      spectrumBuffer = spectrumBuilderBuffer.map { sb =>
        sb.result()
      }
    } else {
      // Put the loaded spectra in the priority queue
      spectrumBuilderBuffer.foreach { sb =>
        priorityQueue.add(sb.result())
        spectrumBuildersCache += sb
      }

      if (continueSlicesLoading) this.initSpectrumSliceBuffer()
    }

  }

  override def next(): Spectrum = { // firstSpectrumSlices is not null
    if (this.iteratorConsumed)
      throw new java.util.NoSuchElementException("no more spectrum available")

    var spectrum: Spectrum = null
    var noMoreSpectrumInBuffer = false

    if (usePriorityQueue) {
      spectrum = priorityQueue.poll
      noMoreSpectrumInBuffer = priorityQueue.isEmpty
    }
    else {
      val spectrumIdxCopy = spectrumBufferReadIdx
      spectrumBufferReadIdx += 1

      spectrum = spectrumBuffer(spectrumIdxCopy)
      noMoreSpectrumInBuffer = spectrumBufferReadIdx == spectrumBuffer.length
    }

    // If no more spectrum in the buffer
    if (noMoreSpectrumInBuffer) {
      if (usePriorityQueue) {
        // clean spectrum builders cache but keep the laste entry
        val lastSpectrumBuilder = spectrumBuildersCache.last
        spectrumBuildersCache.clear()
        spectrumBuildersCache += lastSpectrumBuilder
      } else {
        spectrumBuildersCache.clear()
        spectrumBuildersCache += spectrumBuilderBuffer.last
      }

      if (bbHasNext) {
        initSpectrumSliceBuffer()
      }
      // ending iterator internals
      else {
        this.firstBBReader = null
        this.iteratorConsumed = true
        /*this.spectrumBuilderBuffer.foreach { sb =>
          spectrumDataBuilderFactory.releaseBuilder(sb.spectrumDataBuilder)
        }*/
        this.spectrumBuilderBuffer.clear()
        this.spectrumBuffer.clear()
      }
    }

    spectrum
  }

  def releaseSpectrumData(spectrumId: Long): Unit = {
    val sbPool = if (usePriorityQueue) spectrumBuildersCache else spectrumBuilderBuffer
    val sbOpt = sbPool.find(_.header.id == spectrumId).orElse(spectrumBuildersCache.find(_.header.id == spectrumId))
    //assert(sbOpt.nonEmpty, "unable to retrieve spectrum builder for spectrum id=" + spectrumId)

    if (sbOpt.isEmpty) {
      println(spectrumBuilderBuffer.length)
      println( "unable to retrieve spectrum builder for spectrum id=" + spectrumId)
    }
    else {
      //println("Releasing data of spectrum with id = " + spectrumId)
      spectrumDataBuilderFactory.releaseBuilder(sbOpt.get.spectrumDataBuilder)
    }

    ()
  }
}