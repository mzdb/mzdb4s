package com.github.mzdb4s.io.reader.cache

import com.github.mzdb4s.msdata._

import scala.collection.mutable

/**
  * @author David Bouyssie
  *
  */
class MzDbEntityCache {
  var ms1SpectrumHeaders: Array[SpectrumHeader] = _
  var ms1SpectrumHeaderById: mutable.LongMap[SpectrumHeader] = _
  var ms2SpectrumHeaders: Array[SpectrumHeader] = _
  var ms2SpectrumHeaderById: mutable.LongMap[SpectrumHeader] = _
  var ms3SpectrumHeaders: Array[SpectrumHeader] = _
  var ms3SpectrumHeaderById: mutable.LongMap[SpectrumHeader] = _
  var spectrumHeaders: Array[SpectrumHeader] = _
  var spectrumHeaderById: mutable.LongMap[SpectrumHeader] = _
  var spectrumTimeById: mutable.LongMap[Float] = _
  var spectrumIdsByTimeIndex: mutable.LongMap[mutable.ArrayBuffer[Long]] = _
  var dataEncodingById: mutable.LongMap[DataEncoding] = _
  var dataEncodingBySpectrumId: mutable.LongMap[DataEncoding] = _
  var runSliceHeaders: Array[RunSliceHeader] = _
  var runSliceHeaderById: mutable.LongMap[RunSliceHeader] = _
}

trait IMzDbEntityCacheContainer {

  def entityCache: Option[MzDbEntityCache]

  @inline
  protected lazy val entityCacheOrNull: MzDbEntityCache = entityCache.orNull

  /**
    * @return reader
    */
  //def getMzDbReader: Nothing
}