package com.github.mzdb4s.io.reader.cache

import java.nio.ByteOrder

import scala.collection.mutable.LongMap

import com.github.mzdb4s.db.table.{DataEncodingTable, SpectrumTable}
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.MzDbReaderQueries
import com.github.mzdb4s.io.reader.param.ParamTreeParser
import com.github.mzdb4s.msdata._
import com.github.sqlite4s.ISQLiteRecordExtraction
import com.github.sqlite4s.query.SQLiteRecord

object AbstractDataEncodingReader {
  protected val dataEncodingQueryStr = "SELECT * FROM data_encoding"

  protected def createDataEncoding(record: SQLiteRecord, mzIntPrecisions: (Int,Int)): DataEncoding = {
    val id = record.columnInt(DataEncodingTable.ID)
    val dmAsStr = record.columnString(DataEncodingTable.MODE)
    val compression = record.columnString(DataEncodingTable.COMPRESSION)
    val byteOrderAsStr = record.columnString(DataEncodingTable.BYTE_ORDER)

    // Parse record values
    val dm = if (dmAsStr == "centroided") DataMode.CENTROID else DataMode.withName(dmAsStr) // "centroided" was used in old mzDB files
    val bo = if (byteOrderAsStr.equalsIgnoreCase("big_endian")) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

    val (mzPrecision, intPrecision) = mzIntPrecisions

    val peakEnc = if (mzPrecision == 32) PeakEncoding.LOW_RES_PEAK
    else if (intPrecision == 32) PeakEncoding.HIGH_RES_PEAK
    else PeakEncoding.NO_LOSS_PEAK

    // Return data encoding object
    DataEncoding(id, dm, peakEnc, compression, bo)
  }
}

abstract class AbstractDataEncodingReader(val entityCache: Option[MzDbEntityCache]) extends IMzDbEntityCacheContainer {

  private var _dataEncodingExtractor: ISQLiteRecordExtraction[DataEncoding] = null

  private def _getOrCreateDataEncodingExtractor()(implicit mzDbCtx: MzDbContext): ISQLiteRecordExtraction[DataEncoding] = {
    if (_dataEncodingExtractor != null) return _dataEncodingExtractor

    val modelVersion = MzDbReaderQueries.getModelVersion()

    // Check if model version is newer than 0.6
    _dataEncodingExtractor = if (modelVersion.compareTo("0.6") > 0) new Object with ISQLiteRecordExtraction[DataEncoding] {
      def extractRecord(record: SQLiteRecord): DataEncoding = {
        AbstractDataEncodingReader.createDataEncoding(
          record,
          (record.columnInt(DataEncodingTable.MZ_PRECISION),record.columnInt(DataEncodingTable.INTENSITY_PRECISION))
        )
      }
    }
    else new Object with ISQLiteRecordExtraction[DataEncoding] {
      def extractRecord(record: SQLiteRecord): DataEncoding = {
        AbstractDataEncodingReader.createDataEncoding(
          record,
          {
            // Parse param tree
            val paramTreeAsStr = record.columnString(SpectrumTable.PARAM_TREE)
            val paramTree = ParamTreeParser.parseParamTree(paramTreeAsStr)
            // NOTE: the two CV params may have the same AC => it could be conflicting...
            // It has been in fixed in version 0.9.8 of pwiz-mzdb
            val cvParams = paramTree.getCVParams()
            val mzPrecision = cvParams(0).getValue.toInt
            val intPrecision = cvParams(1).getValue.toInt
            (mzPrecision, intPrecision)
          }
        )
      }
    }

    _dataEncodingExtractor
  }

  protected[cache] def getDataEncoding(dataEncodingId: Int)(implicit mzDbCtx: MzDbContext): DataEncoding = if (this.entityCache.nonEmpty) this.getDataEncodingById().apply(dataEncodingId)
  else { // Retrieve data encoding record
    val queryStr = AbstractDataEncodingReader.dataEncodingQueryStr + " WHERE id = ?"
    mzDbCtx.newSQLiteQuery(queryStr).bind(1, dataEncodingId).extractRecord(this._getOrCreateDataEncodingExtractor())
  }

  protected def getDataEncodings()(implicit mzDbCtx: MzDbContext): Seq[DataEncoding] = {
    mzDbCtx.newSQLiteQuery(AbstractDataEncodingReader.dataEncodingQueryStr).extractRecordList(this._getOrCreateDataEncodingExtractor())
  }

  protected def getDataEncodingById()(implicit mzDbCtx: MzDbContext): LongMap[DataEncoding] = {
    if (this.entityCache.nonEmpty && entityCacheOrNull.dataEncodingById != null) entityCacheOrNull.dataEncodingById
    else {
      val dataEncodings = this.getDataEncodings()

      val dataEncodingById = new LongMap[DataEncoding](dataEncodings.length)
      for (dataEncoding <- dataEncodings) {
        dataEncodingById.put(dataEncoding.getId, dataEncoding)
      }

      if (this.entityCache.nonEmpty) entityCacheOrNull.dataEncodingById = dataEncodingById

      dataEncodingById
    }
  }

  def getDataEncodingBySpectrumId()(implicit mzDbCtx: MzDbContext): LongMap[DataEncoding] = {
    if (this.entityCache.nonEmpty && entityCacheOrNull.dataEncodingBySpectrumId != null) entityCacheOrNull.dataEncodingBySpectrumId
    else {
      val dataEncodingById = this.getDataEncodingById()

      // Retrieve the number of spectra
      val spectraCount = MzDbReaderQueries.getSpectraCount()

      // Retrieve encoding PK for the given spectrum id
      val queryStr = "SELECT id, data_encoding_id FROM spectrum"
      val records = mzDbCtx.newSQLiteQuery(queryStr).getRecordIterator()
      val dataEncodingBySpectrumId = new LongMap[DataEncoding](spectraCount)

      while (records.hasNext) {
        val record = records.next
        val spectrumId = record.columnLong(SpectrumTable.ID)
        val spectrumDataEncodingId = record.columnInt(SpectrumTable.DATA_ENCODING_ID)
        val dataEnc = dataEncodingById(spectrumDataEncodingId)

        /*
        // Looking for the appropriate peak encoding
        // FIXME: retrieve the resolution from the data encoding param tree
        PeakEncoding pe = (h.isHighResolution()) ? PeakEncoding.HIGH_RES_PEAK : PeakEncoding.LOW_RES_PEAK;
        if (mzDbReader.isNoLossMode())
          pe = PeakEncoding.NO_LOSS_PEAK;

        // Setting new peak encoding was set to null before
        dataEnc.setPeakEncoding(pe)
        */

        dataEncodingBySpectrumId.put(spectrumId, dataEnc)
      }

      if (this.entityCache.nonEmpty) this.entityCache.get.dataEncodingBySpectrumId = dataEncodingBySpectrumId

      dataEncodingBySpectrumId
    }
  }

  protected def getSpectrumDataEncoding(spectrumId: Long)(implicit mzDbCtx: MzDbContext): DataEncoding = {
    if (this.entityCache != null) this.getDataEncodingBySpectrumId().apply(spectrumId)
    else {
      val queryStr = s"SELECT data_encoding_id FROM spectrum WHERE id = $spectrumId"
      this.getDataEncoding(mzDbCtx.newSQLiteQuery(queryStr).extractSingleInt())
    }
  }
}