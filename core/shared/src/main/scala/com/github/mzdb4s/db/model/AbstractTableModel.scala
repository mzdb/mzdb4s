package com.github.mzdb4s.db.model

//import java.util
//import com.almworks.sqlite4java.SQLiteConnection
//import com.almworks.sqlite4java.SQLiteException

//import fr.profi.mzdb.io.reader.table.ParamTreeParser
import com.github.mzdb4s.util.atomic.InMemoryIdGen
import com.github.mzdb4s.io.MzDbContext
import params._
import param._

abstract class AbstractTableModel[IdType] protected(
  protected var _paramTree: ParamTree
) extends IParamContainer with InMemoryIdGen {

  //type IdType = Int
  def id: IdType
  def tableName(): String

  //protected var paramTree: ParamTree = _

  def hasParamTree(): Boolean = _paramTree != null

  def getOrLoadParamTree()(implicit mzdbCtx: MzDbContext): ParamTree = {
    if (!this.hasParamTree) _paramTree = mzdbCtx.loadParamTree(this.tableName())
    _paramTree
  }
  def getParamTree(): Option[ParamTree] = Option(_paramTree)

  def setParamTree(paramTree: ParamTree): this.type = {
    this._paramTree = paramTree
    this
  }

  /*protected def loadParamTree(mzDbConnection: ISQLiteConnection)(implicit sf: ISQLiteFactory): Unit = {
    val sqlString = "SELECT param_tree FROM " + this.tableName()
    val paramTreeAsStr = new SQLiteQuery(mzDbConnection, sqlString).extractSingleString()
    this.paramTree = ParamTreeParser.parseParamTree(paramTreeAsStr)
  }*/

  // FIXME: shall we call getParamTree instead to trigger the data loading???
  def getCVParams(): Seq[CVParam] = this._paramTree.getCVParams()

  def getUserParams(): Seq[UserParam] = this._paramTree.getUserParams()

  def getUserParam(name: String): UserParam = this._paramTree.getUserParam(name)

  def getUserTexts(): Seq[UserText] = this._paramTree.getUserTexts()
}