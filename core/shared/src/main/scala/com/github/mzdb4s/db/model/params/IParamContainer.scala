package com.github.mzdb4s.db.model.params

//import java.util
//import javax.xml.bind.annotation.XmlElement
import param._

/**
  * The trait IParamContainer.
  *
  * @author David Bouyssie
  */
trait IParamContainer {
  //@XmlElement(name = "cvParam", `type` = classOf[Nothing], required = false)
  def getCVParams(): Seq[CVParam]

  /**
    * Gets the user params.
    *
    * @return the user params
    */
  //@XmlElement(name = "userParam", `type` = classOf[Nothing], required = false)
  def getUserParams(): Seq[UserParam]

  /**
    * Gets the user texts.
    *
    * @return the user texts
    */
  //@XmlElement(name = "userText", `type` = classOf[Nothing], required = false)
  def getUserTexts(): Seq[UserText]

  /**
    * Gets the user param.
    *
    * @param name
    * the name
    * @return the user param
    */
  def getUserParam(name: String): UserParam
}