package com.github.mzdb4s.db.model.params.param

//import javax.xml.bind.annotation._

//@XmlAccessorType(XmlAccessType.FIELD)
// TODO: DBO => upgrade to a Scala case class???
class CVParam extends AbstractParam {

  /*//@XmlAttribute
  protected var cvRef: String = _
  //@XmlAttribute
  protected var accession: String = _
  //@XmlAttribute
  protected var name: String = _*/

  //@XmlAttribute
  protected var value: String = _
  //@XmlAttribute
  protected var unitCvRef: String = _
  //@XmlAttribute
  protected var unitAccession: String = _
  //@XmlAttribute
  protected var unitName: String = _

  /*def getCvRef: String = cvRef

  def getAccession: String = accession

  def getName: String = name*/

  override def getValue(): String = value

  def getUnitCvRef(): String = unitCvRef

  def getUnitAccession(): String = unitAccession

  def getUnitName(): String = unitName

  /*
  def setCvRef(cvRef: String): this.type = {
    this.cvRef = cvRef
    this
  }

  def setAccession(accession: String): this.type = {
    this.accession = accession
    this
  }

  def setName(name: String): this.type = {
    this.name = name
    this
  }*/

  override def setValue(value: String): this.type = {
    this.value = value
    this
  }

  def setUnitCvRef(unitCvRef: String): this.type = {
    this.unitCvRef = unitCvRef
    this
  }

  def setUnitAccession(unitAccession: String): this.type = {
    this.unitAccession = unitAccession
    this
  }

  def setUnitName(unitName: String): this.type = {
    this.unitName = unitName
    this
  }
}