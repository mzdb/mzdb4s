package com.github.mzdb4s.db.table

abstract class AbstractTableDefinition extends Enumeration {

  /*protected final def Column(name: String): Column = new Column(this.nextId, name.toLowerCase())
  class Column(i: Int, name: String) extends Val(i: Int, name: String) {
    def getName(): String = name
    override def toString(): String = this.name
  }*/

  def tableName: String

  /*protected final def Column: Column = new Column(this.nextId)
  class Column(i: Int) extends Val(i: Int) {
    protected lazy val name = super.toString().toLowerCase
    def getName(): String = name
  }*/

  protected final def Column(name: String): Column = new Column(name)
  class Column(name: String) extends Val(name) {
    def getIndex(): Int = id
    def getName(): String = name
  }
}
