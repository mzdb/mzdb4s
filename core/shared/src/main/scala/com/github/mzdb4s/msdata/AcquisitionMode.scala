package com.github.mzdb4s.msdata

/**
  * Enumeration representing the acquisition mode. It is stored as a cvParam in the run table.
  * This list is NOT exhaustive:
  *  - DDA meaning Data Dependant Acquisition
  *  - DDA=IDA
  *  - and DIA=SWATH
  *  - DDA/DIA are Thermo terms
  *  - IDA/SWATH are AbSciex terms
  */
object AcquisitionMode extends Enumeration {

  val DDA = Mode(
    "DDA acquisition",
    "Data Dependant Acquisition (Thermo designation), Warning: in ABI this is called IDA (Information Dependant Acquisition)"
  )
  // TODO: add DIA mode
  val SWATH = Mode(
    "SWATH acquisition",
    "ABI Swath acquisition or Thermo DIA acquisition"
  )
  val MRM = Mode(
    "MRM acquisition",
    "Multiple reaction monitoring"
  )
  val SRM = Mode("SRM acquisition", "Single reaction monitoring")
  val UNKNOWN = Mode(
    "UNKNOWN acquisition",
    "unknown acquisition mode"
  )

  protected final def Mode(name: String, description: String): Mode = new Mode(this.nextId, name, description)
  class Mode(i: Int, name: String, description: String) extends Val(i: Int, name: String) {
    def getName(): String = name
    def getDescription(): String = description
    override def toString(): String = this.name
  }

  def getAcquisitionMode(code: String): Mode = {
    val modeOpt = this.values.collectFirst { case mode: Mode if mode.getName() == code => mode }
    modeOpt.getOrElse(UNKNOWN)
  }
}