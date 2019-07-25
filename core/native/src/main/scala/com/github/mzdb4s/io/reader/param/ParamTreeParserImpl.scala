/*package com.github.mzdb4s.io.reader.param

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

object ParamTreeParserImpl {

  private val cvParamsStartTag = "<cvParams>"
  private val userParamsStartTag = "<userParams>"
  private val userTextsStartTag = "<userTexts>"

  private val CvParamsPattern = "(?s).+<cvParams>(.+)</cvParams>.+".r
  private val UserParamsPattern = "(?s).+<userParams>(.+)</userParams>.+".r
  private val UserTextsPattern = "(?s).+<userTexts>(.+)</userTexts>.+".r

  // FIXME: implement parsing of units
  private val CvParamPattern = """(?s).+cvRef="(\w+)" accession="(.+)" name="(.+)" value="(.+)".*""".r
  private val UserParamPattern = """(?s).+cvRef="(\w+)" accession="(.+)" name="(.+)" type="(.+)" value="(.*)".*""".r
  //private val UserTextPattern = "(?s).+<userTexts>(.+)</userTexts>".r

  def parseParamTree(paramTreeAsStr: String): ParamTree = {

    val paramTree = new ParamTree()

    if (paramTreeAsStr.contains(cvParamsStartTag)) {
      val CvParamsPattern(cvParamsStr) = paramTreeAsStr
      val cvParamsAsStr = cvParamsStr.split("/>")
      val cvParams = cvParamsAsStr.take(cvParamsAsStr.length - 1).map { cvParamStr =>
        val CvParamPattern(cvRef,accession,name,value) = cvParamStr
        val cvParam = new CVParam()
        cvParam
          .setCvRef(cvRef)
          .setAccession(accession)
          .setName(name)
          .setValue(value)

        cvParam
      }

      paramTree.setCvParams(cvParams)
    }

    if (paramTreeAsStr.contains(userParamsStartTag)) {
      val UserParamsPattern(userParamsStr) = paramTreeAsStr
      val userParamsAsStr = userParamsStr.split("/>")
      val userParams = userParamsAsStr.take(userParamsAsStr.length - 1).map { userParamStr =>
        val UserParamPattern(cvRef,accession,name,valueType,value) = userParamStr
        val userParam = new UserParam()
        userParam
          .setCvRef(cvRef)
          .setAccession(accession)
          .setName(name)
          .setType(valueType)
          .setValue(value)

        userParam
      }

      paramTree.setUserParams(userParams)
    }

    /*if (paramTreeAsStr.contains(userTextsStartTag)) {
      val UserTextsPattern(userTexts) = paramTreeAsStr
      //println(userTexts)
    }*/


    paramTree
  }

  /*
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

    /*var paramTree = null
    try {
      if (paramTreeUnmarshaller == null) paramTreeUnmarshaller = JAXBContext.newInstance(classOf[Nothing]).createUnmarshaller
      val source = XercesSAXParser.getSAXSource(paramTreeAsStr)
      paramTree = paramTreeUnmarshaller.unmarshal(source).asInstanceOf[Nothing]
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    paramTree*/

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
  }*/

  def parseScanList(scanListAsStr: String): ScanList = {
    null
  }

  def parsePrecursor(precursorAsStr: String): Precursor = {
    null
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }
}*/