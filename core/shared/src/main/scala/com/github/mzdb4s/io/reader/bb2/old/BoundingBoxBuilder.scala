/*package com.github.mzdb4s.io.reader.bb2


import java.io.InputStream

import scala.collection.mutable.LongMap
import com.github.mzdb4s.msdata._
import com.github.sqlite4s._

/**
  * The Class BoundingBoxBuilder.
  * <p>
  * Contains static methods to build BoundingBox objects Use a different reader depending of provided data in
  * the constructor
  * </p>
  *
  * @author David Bouyssie
  */
object BoundingBoxBuilder {

  //@throws[StreamCorruptedException]
  def buildBB(
    bbId: Int,
    bytes: Array[Byte],
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  ): BoundingBox = {
    val bb = new BoundingBox(bbId, new BytesReader(bytes, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId))
    bb.setFirstSpectrumId(firstSpectrumId)
    bb.setLastSpectrumId(lastSpectrumId)
    bb
  }

  //@throws[StreamCorruptedException]
  def buildBB(
    bbId: Int,
    blob: ISQLiteBlob,
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  ): BoundingBox = {
    val bb = new BoundingBox(bbId, new SQLiteBlobReader(blob, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId))
    bb.setFirstSpectrumId(firstSpectrumId)
    bb.setLastSpectrumId(lastSpectrumId)
    bb
  }

  def buildBB(
    bbId: Int,
    stream: InputStream,
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  ): BoundingBox = {
    val bb = new BoundingBox(bbId, new StreamReader(stream, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId))
    bb.setFirstSpectrumId(firstSpectrumId)
    bb.setLastSpectrumId(lastSpectrumId)
    bb
  }
}*/