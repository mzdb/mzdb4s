package com.github.mzdb4s.db.model.params.param

abstract class AbstractParam {

  protected var cvRef: String = _
  protected var accession: String = _
  protected var name: String = _

  def getCvRef(): String = cvRef
  def getAccession(): String = accession
  def getName(): String = name
  def getValue(): String

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
  }

  def setValue(value: String): this.type

}