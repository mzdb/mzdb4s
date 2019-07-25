package com.github.mzdb4s.db.model.params.param

/*import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute*/

//@XmlAccessorType(XmlAccessType.FIELD)
// TODO: DBO => upgrade to a Scala case class???
class UserParam extends AbstractParam {

  /*
  //@XmlAttribute
  protected var cvRef: String = _
  //@XmlAttribute
  protected var accession: String = _
  //@XmlAttribute
  protected var name: String = _*/

  //@XmlAttribute
  protected var value: String = _
  //@XmlAttribute
  protected var `type`: String = _ // ="xsd:float"/>;

  /*def getCvRef: String = cvRef

  def getAccession: String = accession

  def getName: String = name*/

  override def getValue(): String = value

  def getType(): String = `type`

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

  def setType(`type`: String): this.type = {
    this.`type` = `type`
    this
  }
}