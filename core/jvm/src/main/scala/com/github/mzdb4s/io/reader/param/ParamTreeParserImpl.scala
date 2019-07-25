/*package com.github.mzdb4s.io.reader

import pine._

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

class RichTag[T <: Singleton](val tag: Tag[T]) extends AnyVal  {
  def findFirstChild(fn: Tag[T] => Boolean): Option[Tag[T]] = {
    tag.children.collectFirst({case x: Tag[T] if fn(x) => x })
  }
  def findFirstChildNamed(name: String): Option[Tag[T]] = {
    tag.children.collectFirst({case x: Tag[T] if x.tagName == name => x })
  }
  def filterChildren(fn: Tag[T] => Boolean): List[Tag[T]] = {
    tag.children.collect({case x: Tag[T] if fn(x) => x })
  }
}

object ParamTreeParserImpl {

  implicit def tag2richTag[T <: Singleton](tag: Tag[T]): RichTag[T] = new RichTag[T](tag)

  def parseParamTree(paramTreeAsStr: String): ParamTree = {

    //val xmlTree = pine.XmlParser.fromString(paramTreeAsStr)
    val xmlTree = pine.internal.HtmlParser.fromString(paramTreeAsStr, xml = true)
    val cvParamsTreeOpt = xmlTree.findFirstChildNamed("cvParams")
    val userParamsTreeOpt = xmlTree.findFirstChildNamed("userParams")
    val userTextsTreeOpt = xmlTree.findFirstChildNamed("userTexts")

    val paramTree = new ParamTree()

    if (cvParamsTreeOpt.isDefined) {
      val cvParams = cvParamsTreeOpt.get.filterChildren(_.tagName == "cvParam").map { cvParam =>
        _parseCvParam(cvParam.attributes)
      }
      paramTree.setCvParams(cvParams)
    }

    if (userParamsTreeOpt.isDefined) {
      val userParams = userParamsTreeOpt.get.filterChildren(_.tagName == "userParam").map { userParam =>
        _parseUserParam(userParam.attributes)
      }
      paramTree.setUserParams(userParams)
    }

    if (userTextsTreeOpt.isDefined) {
      val userTexts = userTextsTreeOpt.get.filterChildren(_.tagName == "userText").map { userText =>
        _parseUserText(userText.attributes)
      }
      paramTree.setUserTexts(userTexts)
    }

    paramTree
  }

  private def _parseAbstractParam[T <: AbstractParam](attributes: collection.Map[String,String], param: T): T = {
    param
      .setCvRef(attributes.get("cvRef").orNull)
      .setAccession(attributes.get("accession").orNull)
      .setName(attributes.get("name").orNull)
  }

  private def _parseCvParam(attributes: collection.Map[String,String]): CVParam = {
    _parseAbstractParam(attributes, new CVParam())
      .setValue(attributes.get("value").orNull)
      .setUnitCvRef(attributes.get("unitCvRef").orNull)
      .setUnitAccession(attributes.get("unitAccession").orNull)
      .setUnitName(attributes.get("unitName").orNull)
  }

  private def _parseUserParam(attributes: collection.Map[String,String]): UserParam = {
    _parseAbstractParam(attributes, new UserParam())
      .setValue(attributes.get("value").orNull)
      .setType(attributes.get("type").orNull)
  }

  private def _parseUserText(attributes: collection.Map[String,String]): UserText = {
    _parseAbstractParam(attributes, new UserText())
      .setText(attributes.get("value").orNull)
      .setType(attributes.get("type").orNull)
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    null
  }

  def parsePrecursor(precursorAsStr: String): Precursor = {
    null
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }
}
*/

/*

object JAXBParamTreeParserImpl {

  /** The xml mappers. */
  var paramTreeUnmarshaller: Unmarshaller = null
  var componentListUnmarshaller: Unmarshaller = null
  var scanListUnmarshaller: Unmarshaller = null
  var precursorUnmarshaller: Unmarshaller = null

  def parseParamTree(paramTreeAsStr: String): ParamTree = {

    var paramTree = null
    try {
      if (paramTreeUnmarshaller == null) paramTreeUnmarshaller = JAXBContext.newInstance(classOf[Nothing]).createUnmarshaller
      val source = XercesSAXParser.getSAXSource(paramTreeAsStr)
      paramTree = paramTreeUnmarshaller.unmarshal(source).asInstanceOf[Nothing]
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    paramTree
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    var scanList = null
    try {
      if (scanListUnmarshaller == null) scanListUnmarshaller = JAXBContext.newInstance(classOf[Nothing]).createUnmarshaller
      val source = XercesSAXParser.getSAXSource(scanListAsStr)
      scanList = scanListUnmarshaller.unmarshal(source).asInstanceOf[Nothing]
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    scanList
  }

  def parsePrecursor(precursorAsStr: String): Precursor = {
    var prec = null
    try {
      if (precursorUnmarshaller == null) precursorUnmarshaller = JAXBContext.newInstance(classOf[Nothing]).createUnmarshaller
      val source = XercesSAXParser.getSAXSource(precursorAsStr)
      prec = precursorUnmarshaller.unmarshal(source).asInstanceOf[Nothing]
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    prec
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    var paramTree = null
    try {
      if (componentListUnmarshaller == null) componentListUnmarshaller = JAXBContext.newInstance(classOf[Nothing]).createUnmarshaller
      val source = XercesSAXParser.getSAXSource(componentListAsStr)
      paramTree = componentListUnmarshaller.unmarshal(source).asInstanceOf[Nothing]
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    paramTree
  }
}
*/