package com.github.mzdb4s.msdata

import java.util.Comparator

import scala.beans.BeanProperty

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.io.MzDbContext


object SpectrumHeader {

  /** The rt comp. */
  var rtComp: Comparator[SpectrumHeader] = new Comparator[SpectrumHeader]() { // @Override
    override def compare(o1: SpectrumHeader, o2: SpectrumHeader): Int = if (o1.time < o2.time) -1
    else if (Math.abs(o1.time - o2.time) < 1e-6) 0
    else 1
  }

}

case class SpectrumHeader(
  @BeanProperty id: Long,
  @BeanProperty initialId: Int,
  @BeanProperty title: String,
  @BeanProperty cycle: Int,
  @BeanProperty time: Float,
  @BeanProperty msLevel: Int,
  @BeanProperty activationType: Option[ActivationType.Value],
  @BeanProperty peaksCount: Int,
  @BeanProperty isHighResolution: Boolean,
  @BeanProperty tic: Float,
  @BeanProperty basePeakMz: Double,
  @BeanProperty basePeakIntensity: Float,
  @BeanProperty precursorMz: Option[Double],
  @BeanProperty precursorCharge: Option[Int],
  @BeanProperty bbFirstSpectrumId: Long,
  @BeanProperty var scanList: ScanList = null, // TODO: change to Option[ScanList]
  @BeanProperty var precursor: Precursor = null, // TODO: change to Option[Precursor]
  @BeanProperty var isolationWindow: Option[IsolationWindow] = None
) extends AbstractTableModel[Long](null) with ILcContext {

  override def tableName(): String = "spectrum"

  override def getSpectrumId(): Long = this.id

  override def getElutionTime(): Float = this.time

  override def getOrLoadParamTree()(implicit mzdbCtx: MzDbContext): ParamTree = {
    if (!this.hasParamTree) {
      val paramTree = mzdbCtx.loadParamTreeById(this.tableName(), this.id)
      this.setParamTree(paramTree)
      paramTree
    } else super.getParamTree().get
  }

  def getOrLoadScanList()(implicit mzdbCtx: MzDbContext): ScanList = {
    if (scanList == null) scanList = mzdbCtx.loadScanList(this.id)
    scanList
  }

  def getOrLoadPrecursor()(implicit mzdbCtx: MzDbContext): Precursor = {
    if (precursor == null) precursor = mzdbCtx.loadPrecursorList(this.id).headOption.orNull
    precursor
  }

  /*def loadParamTree(mzDbConnection: SQLiteConnection): Unit = if (!this.hasParamTree) {
    val sqlString = "SELECT param_tree FROM spectrum WHERE id = ?"
    val paramTreeAsStr = new Nothing(mzDbConnection, sqlString).bind(1, this.getId).extractSingleString
    this.paramTree = ParamTreeParser.parseParamTree(paramTreeAsStr)
  }*/

  /*
  def loadScanList(mzDbConnection: SQLiteConnection): Unit = if (scanList == null) {
    val sqlString = "SELECT scan_list FROM spectrum WHERE id = ?"
    val scanListAsStr = new Nothing(mzDbConnection, sqlString).bind(1, this.getId).extractSingleString
    this.scanList = ParamTreeParser.parseScanList(scanListAsStr)
  }

  def loadPrecursorList(mzDbConnection: SQLiteConnection): Unit = if (precursor == null) {
    val sqlString = "SELECT precursor_list FROM spectrum WHERE id = ?"
    val precursorListAsStr = new Nothing(mzDbConnection, sqlString).bind(1, this.getId).extractSingleString
    this.precursor = ParamTreeParser.parsePrecursor(precursorListAsStr)
  }*/
}