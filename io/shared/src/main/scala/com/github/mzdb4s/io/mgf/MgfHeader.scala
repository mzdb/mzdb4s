package com.github.mzdb4s.io.mgf

object MgfField extends Enumeration {
  val BEGIN_IONS = Value("BEGIN IONS")
  val END_IONS = Value("END IONS")
  val TITLE, PEPMASS, CHARGE, RTINSECONDS, RAWSCANS, SCANS = Value
}

object MgfHeader {
  private[mgf] val LINE_SEPARATOR: String = System.getProperty("line.separator")
}

case class MgfHeader(entries: collection.Seq[MgfHeaderEntry]) {

  def appendToStringBuilder(sb: StringBuilder): StringBuilder = {
    sb.append(MgfField.BEGIN_IONS).append(MgfHeader.LINE_SEPARATOR)

    for (entry <- entries) {
      entry.appendToStringBuilder(sb).append(MgfHeader.LINE_SEPARATOR)
    }

    sb
  }

  override def toString: String = {
    val sb = new StringBuilder
    this.appendToStringBuilder(sb).toString
  }
}

case class MgfHeaderEntry(field: MgfField.Value, value: Any, trailer: Option[String] = None) {

  def appendToStringBuilder(sb: StringBuilder): StringBuilder = {
    sb.append(field).append("=").append(value)
    if (this.trailer.isDefined) sb.append(trailer.get)
    sb
  }

  override def toString: String = {
    this.appendToStringBuilder(new StringBuilder()).toString
  }
}

