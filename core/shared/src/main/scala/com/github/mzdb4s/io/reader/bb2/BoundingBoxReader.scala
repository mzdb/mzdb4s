package com.github.mzdb4s.io.reader.bb2

import com.github.mzdb4s.msdata._
import com.github.sqlite4s._

import scala.collection.mutable.LongMap

/**
  * The object BoundingBoxReader.
  * <p>
  * Contains static methods to build BoundingBoxReader objects.
  * Use a different reader depending of provided data in the constructor.
  * </p>
  *
  * @author David Bouyssie
  */
object BoundingBoxReader {

  def apply(
    bbId: Int,
    bytes: Array[Byte],
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  )(implicit bbIdxFactory: BoundingBoxIndexFactory): BoundingBoxBytesReader = {
    new BoundingBoxBytesReader(bbId, bytes, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
  }

  def apply(
    bbId: Int,
    blob: ISQLiteBlob,
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  )(implicit bbIdxFactory: BoundingBoxIndexFactory): BoundingBoxBlobReader = {
    new BoundingBoxBlobReader(bbId, blob, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
  }

}