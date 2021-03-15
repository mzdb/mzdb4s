/*package com.github.mzdb4s.io.reader.bb2

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
    runSliceId: Int,
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  )(implicit bbIdxFactory: BoundingBoxIndexFactory): BoundingBoxBytesReader = {
    new BoundingBoxBytesReader(bbId, bytes, runSliceId, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
  }

  def apply(
    bbId: Int,
    blob: ISQLiteBlob,
    runSliceId: Int,
    firstSpectrumId: Long,
    lastSpectrumId: Long,
    spectrumHeaderById: LongMap[SpectrumHeader],
    dataEncodingBySpectrumId: LongMap[DataEncoding]
  )(implicit bbIdxFactory: BoundingBoxIndexFactory): BoundingBoxBlobReader = {
    new BoundingBoxBlobReader(bbId, blob, runSliceId, firstSpectrumId, lastSpectrumId, spectrumHeaderById, dataEncodingBySpectrumId)
  }

}*/