package com.github.mzdb4s.db.model.params.param

// TODO: move to parent package

import scala.beans.BeanProperty

trait IXmlParam {
  def name: String

  def toXml(): String
}

case class CVParam(
  @BeanProperty accession: String,
  @BeanProperty name: String,
  @BeanProperty value: String = "",
  @BeanProperty cvRef: String = "MS",
  @BeanProperty unitCvRef: Option[String] = None,
  @BeanProperty unitAccession: Option[String] = None,
  @BeanProperty unitName: Option[String] = None
) extends IXmlParam {
  def toXml(): String = {
    if (unitName.isDefined) {
      s"""<cvParam cvRef="$cvRef" accession="$accession" name="$name" value="$value" unitCvRef="${unitCvRef.get}" unitAccession="${unitAccession.get}" unitName="${unitName.get}" />"""
    } else {
      s"""<cvParam cvRef="$cvRef" accession="$accession" name="$name" value="$value" />"""
    }
  }
}

case class UserParam(
  @BeanProperty name: String,
  @BeanProperty value: String,
  @BeanProperty `type`: String
) extends IXmlParam {
  def toXml(): String = {
    s"""<userParam name="$name" value="$value" type="${`type`}" />"""
  }
}


case class UserText(
  @BeanProperty name: String,
  @BeanProperty content: String // node content
) extends IXmlParam {
  def toXml(): String = {
    s"""<userText name="$name" type="xsd:string">$content</userText>"""
    /*
    // FIXME: remove this hack
    if (content.startsWith("<userText")) content
    else s"""<userText name="$name" type="xsd:string" />$content</userText>"""
     */
  }
}