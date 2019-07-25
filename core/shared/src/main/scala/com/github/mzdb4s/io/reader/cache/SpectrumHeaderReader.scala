package com.github.mzdb4s.io.reader.cache

import scala.collection.mutable.LongMap

import com.github.mzdb4s.MzDbReader
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.param.ParamTreeParser
import com.github.mzdb4s.msdata.SpectrumHeader
import com.github.sqlite4s.SQLiteQuery
import com.github.sqlite4s.query.SQLiteRecord

object SpectrumHeaderReader {
  /*def loadParamTrees(spectrumHeaders: Array[SpectrumHeader])(implicit mzdbCtx: MzDbContext): Unit = {
    // TODO: load all param_trees in a single SQL query
    for (header <- spectrumHeaders) {
      if (!header.hasParamTree) header.getParamTree()
    }
  }*/

  private def _loadXmlFieldBySpectrumId(sqlString: String)(implicit mzdbCtx: MzDbContext): LongMap[String] = {
    val xmlFieldBySpecId = new LongMap[String]
    val query = new SQLiteQuery(mzdbCtx.mzDbConnection, sqlString)(mzdbCtx.sf)
    val stmt = query.getStatement

    query.forEachRecord { (elem: SQLiteRecord, idx: Int) =>
      val id = stmt.columnLong(0)
      val xmlField = stmt.columnString(1)
      xmlFieldBySpecId.put(id, xmlField)
    }

    xmlFieldBySpecId
  }

  def loadParamTrees(spectrumHeaders: Array[SpectrumHeader])(implicit mzdbCtx: MzDbContext): Unit = {
      val sqlString = "SELECT id, param_tree FROM spectrum"
    val paramTreeBySpecId = _loadXmlFieldBySpectrumId(sqlString)
    for (header <- spectrumHeaders) {
      if (!header.hasParamTree) {
        val paramTreeAsStr = paramTreeBySpecId(header.getId)
        if (paramTreeAsStr != null) header.setParamTree( ParamTreeParser.parseParamTree(paramTreeAsStr) )
      }
    }
  }

  def loadScanLists(spectrumHeaders: Array[SpectrumHeader])(implicit mzdbCtx: MzDbContext): Unit = {
    val sqlString = "SELECT id, scan_list FROM spectrum"
    val scanListBySpecId = _loadXmlFieldBySpectrumId(sqlString)
    for (header <- spectrumHeaders) {
      if (header.scanList == null) {
        val scanListAsStr = scanListBySpecId(header.getId)
        if (scanListAsStr != null) header.scanList = ParamTreeParser.parseScanList(scanListAsStr)
      }
    }
  }

  def loadPrecursors(spectrumHeaders: Array[SpectrumHeader])(implicit mzdbCtx: MzDbContext): Unit = {
    val sqlString = "SELECT id, precursor_list FROM spectrum"
    val precursorBySpecId = _loadXmlFieldBySpectrumId(sqlString)
    for (header <- spectrumHeaders) {
      if (header.precursor == null) {
        val precursorAsStr = precursorBySpecId(header.getId)
        if (precursorAsStr != null) header.precursor = ParamTreeParser.parsePrecursor(precursorAsStr)
      }
    }
  }
}

class SpectrumHeaderReader(
  val mzDbReader: MzDbReader,
  val dataEncodingReader: AbstractDataEncodingReader
)(implicit val mzDbCtx: MzDbContext) extends AbstractSpectrumHeaderReader(mzDbReader, dataEncodingReader) {

  /** Proxy methods **/

  def getSpectrumHeaders(): Array[SpectrumHeader] = super.getSpectrumHeaders()

  def getSpectrumHeaderById(): LongMap[SpectrumHeader] = super.getSpectrumHeaderById()

  def getMs1SpectrumHeaders(): Array[SpectrumHeader] = super.getMs1SpectrumHeaders()

  def getMs1SpectrumHeaderById(): LongMap[SpectrumHeader] = super.getMs1SpectrumHeaderById()

  def getMs2SpectrumHeaders(): Array[SpectrumHeader] = super.getMs2SpectrumHeaders()

  def getMs2SpectrumHeaderById(): LongMap[SpectrumHeader] = super.getMs2SpectrumHeaderById()

  def getSpectrumHeader(id: Long): SpectrumHeader = super.getSpectrumHeader(id)

  def getSpectrumTimeById(): LongMap[Float] = super.getSpectrumTimeById()

  def getSpectrumHeaderForTime(time: Float, msLevel: Int): SpectrumHeader = super.getSpectrumHeaderForTime(time, msLevel)

  def getSpectrumIdsForTimeRange(minRT: Float, maxRT: Float, msLevel: Int): Array[Long] = {
    super.getSpectrumIdsForTimeRange(minRT, maxRT, msLevel)
  }
}