/*package com.github.mzdb4s.io.reader.iterator

import java.io.StreamCorruptedException
import java.util
import com.almworks.sqlite4java.SQLiteConnection
import com.almworks.sqlite4java.SQLiteException
import com.almworks.sqlite4java.SQLiteStatement
import fr.profi.mzdb.AbstractMzDbReader
import fr.profi.mzdb.io.reader.bb.IBlobReader
import fr.profi.mzdb.io.reader.cache.AbstractSpectrumHeaderReader
import fr.profi.mzdb.model.BoundingBox
import fr.profi.mzdb.model.Spectrum
import fr.profi.mzdb.model.SpectrumSlice
import fr.profi.mzdb.util.sqlite.ISQLiteStatementConsumer
//import static fr.profi.mzdb.utils.lambda.JavaStreamExceptionWrappers.rethrowConsumer;
/**
  * @author Marco
  *
  */
class SpectrumRangeIterator @throws[SQLiteException]
@throws[StreamCorruptedException]
(val mzDbReader: Nothing, val connection: SQLiteConnection, val msLevel: Int, var wantedStartingSpectrumId: Int, var wantedEndingSpectrumId: Int) extends util.Iterator[Nothing] {
  val spectrumHeaderReader: Nothing = mzDbReader.getSpectrumHeaderReader
  //this.mzDbReader = mzDbReader;
  this.bbStartingSpectrumId = spectrumHeaderReader.getSpectrumHeader(wantedStartingSpectrumId, connection).getBBFirstSpectrumId
  this.bbEndingSpectrumId = spectrumHeaderReader.getSpectrumHeader(wantedEndingSpectrumId, connection).getBBFirstSpectrumId
  sqlQuery = "SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id AND spectrum.ms_level= ? AND " + "bounding_box.first_spectrum_id >= " + this.bbStartingSpectrumId + " AND bounding_box.first_spectrum_id <= " + this.bbEndingSpectrumId
  this._iter = new SpectrumRangeIterator#MsSpectrumRangeIteratorImpl(mzDbReader, connection, msLevel)
  private var bbStartingSpectrumId = 0
  private var bbEndingSpectrumId = 0
  private var _iter = null
  private var currentId = 0L
  private var sqlQuery = null
  private var trueLastSpectrumId = 0L

  class MsSpectrumRangeIteratorImpl @throws[SQLiteException]
  @throws[StreamCorruptedException]
  (val mzDbReader: Nothing, val connection: SQLiteConnection, val msLevel: Int) extends AbstractSpectrumSliceIterator(mzDbReader.getSpectrumHeaderReader, mzDbReader.getDataEncodingReader, connection, sqlQuery, msLevel, new Nothing() {
    @throws[SQLiteException]
    def accept(stmt: SQLiteStatement): Unit = stmt.bind(1, msLevel) // Bind msLevel}) with util.Iterator[Nothing] { //super(mzDbReader, sqlQuery, msLevel, rethrowConsumer( (stmt) -> stmt.bind(1, msLevel) ) ); // Bind msLevel
    this.initSpectrumSliceBuffer()
    protected var spectrumSliceIdx = 0
    protected var spectrumSliceBuffer: Array[Nothing] = null
    protected var bbHasNext = true

    protected def initSpectrumSliceBuffer(): Unit = {
      this.spectrumSliceBuffer = this.firstBB.toSpectrumSlices
      this.spectrumSliceIdx = 0
      // Build spectrum slice buffer
      while ( {bbHasNext = boundingBoxIterator.hasNext}) { // bbHasNext=
        val bb = boundingBoxIterator.next
        val bbReader = bb.getReader
        if (bb.getLastSpectrumId > wantedEndingSpectrumId) { // FIXME: DBO => it was (bb.nbSpectra() - 2) when idOfSpectrumAt was based on a 1 value starting index
          // It may cause some issues in the future
          trueLastSpectrumId = bbReader.getSpectrumIdAt(bb.getSpectraCount - 1).asInstanceOf[Long]
        }
        else if (bb.getLastSpectrumId eq wantedEndingSpectrumId) trueLastSpectrumId = bb.getLastSpectrumId
        else {
        }
        val sSlices = bb.toSpectrumSlices.asInstanceOf[Array[Nothing]]
        if (sSlices == null) {
          continue //todo: continue is not supported}
          if (sSlices(0).getSpectrumId eq spectrumSliceBuffer(0).getSpectrumId) {
            var i = 0
            while ( {i < sSlices.length}) {
              spectrumSliceBuffer(i).getData.addSpectrumData(sSlices(i).getData)
              {i += 1; i - 1}
            }
          }
          else { // Keep this bounding box for next iteration
            this.firstBB = bb
            break //todo: break is not supported
          }
        }
      }

      override def next

      =
      { // firstSpectrumSlices is not null
        val c = spectrumSliceIdx
        spectrumSliceIdx += 1
        val sSlice = spectrumSliceBuffer(c)
        if (spectrumSliceIdx == spectrumSliceBuffer.length) if (bbHasNext) initSpectrumSliceBuffer()
        else this.firstBB = null
        if (sSlice.getSpectrumId < wantedStartingSpectrumId) null
        else if (sSlice.getSpectrumId > bbEndingSpectrumId && sSlice.getSpectrumId < wantedEndingSpectrumId) {
          this.firstBB = null
          currentId = sSlice.getSpectrumId
          sSlice
        }
        else if (sSlice.getSpectrumId eq wantedEndingSpectrumId) { // toStop = true;
          currentId = sSlice.getSpectrumId
          sSlice
        }
        else if (sSlice.getSpectrumId > wantedEndingSpectrumId) {
          currentId = null
          null
        }
        else sSlice // do nothing
      }
    }

    override def hasNext: Boolean = {
      if (currentId != null && currentId == trueLastSpectrumId) return false
      true
    }

    override def next: Nothing = {
      var sSlice = _iter.next
      while ( {_iter.hasNext && sSlice == null}) { // && ! toStop) {
        sSlice = _iter.next
      }
      currentId = sSlice.getHeader.getId
      sSlice
    }

    /*
       * (non-Javadoc)
       *
       * @see java.util.Iterator#remove()
       */ override def remove(): Unit = {
    }
  }*/