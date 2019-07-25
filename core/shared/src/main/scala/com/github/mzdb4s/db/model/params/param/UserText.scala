package com.github.mzdb4s.db.model.params.param

/*import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlValue*/

//@XmlAccessorType(XmlAccessType.FIELD)
// TODO: DBO => upgrade to a Scala case class???
class UserText extends AbstractParam {

  /*
  //@XmlAttribute
  protected var cvRef: String = null
  //@XmlAttribute
  protected var accession: String = null
  //@XmlAttribute
  protected var name: String = null
  */

  //@XmlValue
  protected var text: String = null
  //@XmlAttribute
  protected var `type`: String = null

  /*
  def getCvRef: String = cvRef

  def getAccession: String = accession

  def getName: String = name*/

  def getText(): String = text

  override def getValue(): String = text

  def getType(): String = `type`

  /*
  def setCvRef(cvRef: String): Unit = this.cvRef = cvRef

  def setAccession(accession: String): Unit = this.accession = accession

  def setName(name: String): Unit = this.name = name
   */

  def setText(text: String): this.type = {
    this.text = text
    this
  }

  override def setValue(value: String): this.type = {
    this.text = value
    this
  }

  def setType(`type`: String): this.type = {
    this.`type` = `type`
    this
  }
}