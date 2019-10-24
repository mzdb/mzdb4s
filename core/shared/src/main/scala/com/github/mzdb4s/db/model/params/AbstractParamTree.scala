package com.github.mzdb4s.db.model.params

import java.util
//import javax.xml.bind.annotation.XmlElement

import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.db.model.params.param.CVParam
import com.github.mzdb4s.db.model.params.param.PsiMsCV
import com.github.mzdb4s.db.model.params.param.UserParam
import com.github.mzdb4s.db.model.params.param.UserText

/**
  * @author David Bouyssie
  *
  */
//@XmlAccessorType(XmlAccessType.NONE)
abstract class AbstractParamTree extends IParamContainer {
  /** The cv params. */
  protected var cvParams: Seq[CVParam] = _
  /** The user params. */
  protected var userParams: Seq[UserParam] = _
  /**
    * The userText params: newly introduced for handling Thermo metadata in text field
    */
  protected var userTexts: Seq[UserText] = _

  //@XmlElement(name = "cvParam", `type` = classOf[Nothing], required = false)
  //@XmlElementWrapper(name = "cvParams")
  def getCVParams(): Seq[CVParam] = {
    if (this.cvParams == null) this.cvParams = new ArrayBuffer[CVParam]
    cvParams
  }

  // Marc: most of the object does not contain any UserParam,
  // so this is set to be non abstract to avoid to override it in subclasses
  // DBO: why ???
  //@XmlElement(name = "userParam", `type` = classOf[Nothing], required = false)
  //@XmlElementWrapper(name = "userParams")
  def getUserParams(): Seq[UserParam] = {
    if (this.userParams == null) this.userParams = new ArrayBuffer[UserParam]
    this.userParams
  }

  //@XmlElement(name = "userText", `type` = classOf[Nothing], required = false)
  //@XmlElementWrapper(name = "userTexts")
  def getUserTexts(): Seq[UserText] = {
    if (this.userTexts == null) this.userTexts = new ArrayBuffer[UserText]
    this.userTexts
  }

  def setCVParams(cvParams: Seq[CVParam]): Unit = this.cvParams = cvParams

  def setUserParams(userParams: Seq[UserParam]): Unit = this.userParams = userParams

  def setUserTexts(userTexts: Seq[UserText]): Unit = this.userTexts = userTexts

  // TODO: rename findUserParam and return Option boxed value
  def getUserParam(name: String): UserParam = {
    userParams.find(_.getName == name).orNull
  }

  def getCVParam(cvEntry: PsiMsCV.Term): CVParam = {
    this.getCVParams().find(_.getAccession == cvEntry.getAccession).orNull
  }

  def getCVParams(cvEntries: Array[CVParam]): Seq[CVParam] = {
    val acSet = cvEntries.map(_.getAccession).toSet
    this.getCVParams().filter(cv => acSet.contains(cv.getAccession))
  }
}