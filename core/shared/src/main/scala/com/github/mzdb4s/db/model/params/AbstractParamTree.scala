package com.github.mzdb4s.db.model.params

import scala.collection.Seq
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
abstract class AbstractParamTree extends Equals with IParamContainer {

  /** The cv params. */
  protected var cvParams: Seq[CVParam] = _
  /** The user params. */
  protected var userParams: Seq[UserParam] = _
  /**
    * The userText params: newly introduced for handling Thermo metadata in text field
    */
  protected var userTexts: Seq[UserText] = _

  // See: https://stackoverflow.com/questions/7370925/what-is-the-standard-idiom-for-implementing-equals-and-hashcode-in-scala
  override def canEqual(that: Any): Boolean = {
    that.isInstanceOf[AbstractParamTree]
  }

  //Intentionally avoiding the call to super.equals because no ancestor has overridden equals (see note 7 below)
  override def equals(that: Any): Boolean = {
    that match {
      case paramTree: AbstractParamTree =>
        ( (this eq paramTree)                   // optional, but highly recommended sans very specific knowledge about this exact class implementation
          || (paramTree.canEqual(this)    // optional only if this class is marked final
          && hashCode == paramTree.hashCode     // optional, exceptionally execution efficient if hashCode is cached, at an obvious space inefficiency tradeoff
          && cvParams == paramTree.cvParams && userParams == paramTree.userParams && userTexts == paramTree.userTexts
          )
          )
      case _ =>
        false
    }
  }

  // Intentionally avoiding the call to super.hashCode because no ancestor has overridden hashCode
  // See: https://stackoverflow.com/questions/9068154/what-is-the-difference-between-and-hashcode/60748297#60748297
  override def hashCode(): Int = {
    31 * cvParams.## + userParams.## + userTexts.## // notes: 31 is a prime number and null.## returns 0
  }

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

  def getCVParam(cvTerm: PsiMsCV.Term): CVParam = {
    this.getCVParams().find(_.getAccession == cvTerm.getAccession).orNull
  }

  def getCVParams(cvTerms: Array[PsiMsCV.Term]): Seq[CVParam] = {
    val acSet = cvTerms.map(_.getAccession).toSet
    this.getCVParams().filter(cv => acSet.contains(cv.getAccession))
  }

  def getCVParams(cvEntries: Array[CVParam]): Seq[CVParam] = {
    val acSet = cvEntries.map(_.getAccession).toSet
    this.getCVParams().filter(cv => acSet.contains(cv.getAccession))
  }

  def toParamTree(): ParamTree = {
    ParamTree(cvParams,userParams,userTexts)
  }

}