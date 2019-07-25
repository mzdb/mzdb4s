package com.github.mzdb4s.io.reader.cache

import scala.collection.mutable.LongMap

import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.msdata.DataEncoding


class DataEncodingReader(entityCache: Option[MzDbEntityCache] = None)(implicit val mzDbCtx: MzDbContext) extends AbstractDataEncodingReader(entityCache) {

  /** Proxy methods **/
  def getDataEncoding(dataEncodingId: Int): DataEncoding = super.getDataEncoding(dataEncodingId)

  def getDataEncodings(): Seq[DataEncoding] = super.getDataEncodings()

  def getDataEncodingById(): LongMap[DataEncoding] = super.getDataEncodingById()

  def getDataEncodingBySpectrumId(): LongMap[DataEncoding] = super.getDataEncodingBySpectrumId()

  def getSpectrumDataEncoding(spectrumId: Long): DataEncoding = super.getSpectrumDataEncoding(spectrumId)
}
