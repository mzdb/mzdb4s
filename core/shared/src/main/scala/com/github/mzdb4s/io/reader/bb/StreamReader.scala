package com.github.mzdb4s.io.reader.bb

import java.io.IOException
import java.io.InputStream

import scala.collection.mutable.LongMap

import com.github.mzdb4s.msdata._

import com.github.mzdb4s.util.primitive.BytesUtils

/**
  * This class aloow to read a SQLite blob using a stream reader. We process data only in one direction in a
  * sequential way The goal is to request only one time the blob
  *
  * @author marco
  *
  */
class StreamReader(
  /** Stream to read */
  private var _stream: InputStream,
  val firstSpectrumId: Long,
  val lastSpectrumId: Long,
  val spectrumHeaderById: LongMap[SpectrumHeader],
  val dataEncodingBySpectrumId: LongMap[DataEncoding]
) extends AbstractBlobReader { //(firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId) {

  override def disposeBlob(): Unit = {
    try _stream.close()
    catch {
      case e: IOException =>
        throw new Exception("IOException has been caught while closing stream", e)
    }
  }

  override def getSpectraCount(): Int = { // FIXME: this information should be added to the BB to optimize performances
    throw new Exception("NYI") // -1
  }

  /**
    * @see IBlobReader#idOfSpectrumAt(int)
    */
  override def getSpectrumIdAt(idx: Int): Long = {
    var lastSpectrumId = 0L

    var j = 0
    while (j <= idx) {
      val spectrumIdBytes = new Array[Byte](4)
      _stream.read(spectrumIdBytes)
      lastSpectrumId = BytesUtils.bytesToInt(spectrumIdBytes, 0).toLong

      val peaksCountBytes = new Array[Byte](4)
      _stream.read(peaksCountBytes)

      val peaksCount = BytesUtils.bytesToInt(peaksCountBytes, 0)
      val de = this.dataEncodingBySpectrumId(lastSpectrumId)
      this.checkDataEncodingIsNotNull(de, lastSpectrumId)

      _stream.skip(peaksCount * de.getPeakStructSize)

      j += 1
    }

    lastSpectrumId
  }

  override def readSpectrumSliceAt(idx: Int): SpectrumSlice = this._readSpectrumSliceAt(idx, -1.0, -1.0)

  private def _readSpectrumSliceAt(idx: Int, minMz: Double, maxMz: Double): SpectrumSlice = {

    var peaksBytes: Array[Byte] = null
    var spectrumId = 0L
    var peaksCount = 0
    var de: DataEncoding = null

    var j = 0
    while (j <= idx) {
      val spectrumIdBytes = new Array[Byte](4)
      _stream.read(spectrumIdBytes)
      spectrumId = BytesUtils.bytesToInt(spectrumIdBytes, 0).toLong
      de = this.dataEncodingBySpectrumId(spectrumId)

      val peaksCountBytes = new Array[Byte](4)
      _stream.read(peaksCountBytes)
      peaksCount = BytesUtils.bytesToInt(peaksCountBytes, 0)

      val peaksBytesSize = peaksCount * de.getPeakStructSize
      // If not on specified index
      if (j < idx) { // skip the peaks
        _stream.skip(peaksBytesSize)
      }
      else { // read peaks
        val pb = new Array[Byte](peaksBytesSize)
        _stream.read(pb)
        peaksBytes = pb
      }

      j += 1
    }

    if (peaksBytes == null) return null

    val spectrumSliceData = this.readSpectrumSliceData(
      ByteArrayWrapper(peaksBytes, de.byteOrder),
      0,
      peaksBytes.length,
      de,
      minMz,
      maxMz
    )

    SpectrumSlice(spectrumHeaderById(spectrumId), spectrumSliceData)
  }

  // TODO: call this method from readSpectrumSliceAt instead of calling readSpectrumSliceAt from this methods
  override def readSpectrumSliceDataAt(idx: Int): ISpectrumData = this.readSpectrumSliceAt(idx).getData

  override def readFilteredSpectrumSliceDataAt(idx: Int, minMz: Double, maxMz: Double): ISpectrumData = this._readSpectrumSliceAt(idx, minMz, maxMz).getData
}