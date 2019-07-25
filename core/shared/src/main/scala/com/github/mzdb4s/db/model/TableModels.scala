package com.github.mzdb4s.db.model

import java.util.Date

import scala.beans.BeanProperty
import params._

object InstrumentConfiguration {
  val TABLE_NAME = "instrument_configuration"
}

case class InstrumentConfiguration(
  @BeanProperty var id: Int,
  @BeanProperty var name: String,
  @BeanProperty var softwareId: Int,
  protected val paramTree: ParamTree,
  @BeanProperty var componentList: ComponentList
) extends AbstractTableModel[Int](paramTree) {

  def tableName(): String = InstrumentConfiguration.TABLE_NAME

  def this(id: Int, name: String, softwareId: Int) {
    this(id, name, softwareId, null, null)
  }

}

object MzDbHeader {
  val TABLE_NAME = "mzdb"
}

case class MzDbHeader(
  @BeanProperty var version: String,
  @BeanProperty var creationTimestamp: Int,
  protected var paramTree: ParamTree
) extends AbstractTableModel[Int](paramTree) {

  var id = 1

  override def tableName(): String = MzDbHeader.TABLE_NAME

  def this(version: String, creationTimestamp: Int) {
    this(version, creationTimestamp, null)
  }

}

object Run {
  val TABLE_NAME = "run"
}

case class Run(
  @BeanProperty id: Int,
  @BeanProperty var name: String,
  // TODO: change from Date to Instant type
  //protected Instant startTimestamp;
  @BeanProperty var startTimestamp: Date,
  protected var paramTree: ParamTree
) extends AbstractTableModel[Int](paramTree) {

  override def tableName(): String = Run.TABLE_NAME

  def this(id: Int, name: String, startTimestamp: Date) {
    this(id, name, startTimestamp, null)
  }
}

object Sample {
  val TABLE_NAME = "sample"
}

case class Sample(
  @BeanProperty id: Int,
  @BeanProperty var name: String,
  var paramTree: ParamTree
) extends AbstractTableModel[Int](paramTree) {

  override def tableName(): String = Sample.TABLE_NAME

  def this(id: Int, name: String) {
    this(id, name, null)
  }
}

object Software {
  val TABLE_NAME = "software"
}

case class Software(
  @BeanProperty id: Int,
  @BeanProperty var name: String,
  @BeanProperty var version: String,
  protected var paramTree: ParamTree
) extends AbstractTableModel[Int](paramTree) {

  override def tableName(): String = Software.TABLE_NAME

  def this(id: Int, name: String, version: String) {
    this(id, name, version, null)
  }

}

object SourceFile {
  val TABLE_NAME = "source_file"
}

case class SourceFile(
  @BeanProperty id: Int,
  @BeanProperty var name: String,
  @BeanProperty var location: String,
  protected var paramTree: ParamTree
) extends AbstractTableModel[Int](paramTree) {

  override def tableName(): String = SourceFile.TABLE_NAME

  def this(id: Int, name: String, location: String) {
    this(id, name, location, null)
  }

}