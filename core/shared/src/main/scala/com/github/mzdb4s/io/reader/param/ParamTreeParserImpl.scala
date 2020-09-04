package com.github.mzdb4s.io.reader.param

import pine._

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

private[param] class JvmRichTag[T <: Singleton](val tag: Tag[T]) extends AnyVal  {
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

private[param] object ParamTreeParserImpl {

  implicit def tag2richTag[T <: Singleton](tag: Tag[T]): JvmRichTag[T] = new JvmRichTag[T](tag)

  @inline
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
      paramTree.setCVParams(cvParams)
    }

    if (userParamsTreeOpt.isDefined) {
      val userParams = userParamsTreeOpt.get.filterChildren(_.tagName == "userParam").map { userParam =>
        _parseUserParam(userParam.attributes)
      }
      paramTree.setUserParams(userParams)
    }

    if (userTextsTreeOpt.isDefined) {
      val userTexts = userTextsTreeOpt.get.filterChildren(_.tagName == "userText").map { userText =>
        val textOpt = userText.children.collectFirst {
          case pine.Text(t) => t
        }

        _parseUserText(userText.attributes, textOpt.getOrElse(""))
      }
      paramTree.setUserTexts(userTexts)
    }

    paramTree
  }

  private def _parseCvParam(attributes: collection.Map[String,String]): CVParam = {
    CVParam(
      accession = attributes.get("accession").orNull,
      name = attributes.get("name").orNull,
      value = attributes.get("value").orNull,
      cvRef = attributes.get("cvRef").orNull,
      unitCvRef = attributes.get("unitCvRef"),
      unitAccession = attributes.get("unitAccession"),
      unitName = attributes.get("unitName")
    )
  }

  private def _parseUserParam(attributes: collection.Map[String,String]): UserParam = {
    UserParam(
      name = attributes.get("name").orNull,
      value = attributes.get("value").orNull,
      `type` = attributes.get("type").orNull
    )
  }

  private def _parseUserText(attributes: collection.Map[String,String], content: String): UserText = {
    UserText(
      name = attributes.get("name").orNull,
      content = content
    )
  }

  @inline
  def parseScanList(scanListAsStr: String): ScanList = {

    // TODO: put this in unit tests
    """<scanList count="1">
      |  <cvParam cvRef="MS" accession="MS:1000795" value="" name="no combination" />
      |  <scan instrumentConfigurationRef="IC2">
      |    <cvParam cvRef="MS" accession="MS:1000016" value="0.035155" name="scan start time" unitAccession="UO:0000031" unitName="minute" unitCvRef="UO" />
      |    <cvParam cvRef="MS" accession="MS:1000512" value="ITMS + c NSI d Full ms2 776.30@cid30.00 [200.00-2000.00]" name="filter string" />
      |    <cvParam cvRef="MS" accession="MS:1000927" value="100" name="ion injection time" unitAccession="UO:0000028" unitName="millisecond" unitCvRef="UO" />
      |    <userParam name="[Thermo Trailer Extra]Monoisotopic M/Z:" type="xsd:float" value="776.2991" />
      |    <scanWindowList count="1">
      |      <scanWindow>
      |        <cvParam cvRef="MS" accession="MS:1000501" value="200" name="scan window lower limit" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |        <cvParam cvRef="MS" accession="MS:1000500" value="2000" name="scan window upper limit" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |      </scanWindow>
      |    </scanWindowList>
      |  </scan>
      |</scanList>
      |
      |""".stripMargin

    val xmlTree = pine.internal.HtmlParser.fromString(scanListAsStr, xml = true)

    val scansCount = xmlTree.attributes.get("count").map(_.toInt).getOrElse(0)
    val scanList = new ScanList(scansCount)

    val cvParams = xmlTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
      _parseCvParam(cvParam.attributes)
    }
    val userParams = xmlTree.filterChildren(_.tagName == "userParam").map { userParam =>
      _parseUserParam(userParam.attributes)
    }

    scanList.setCVParams(cvParams)
    scanList.setUserParams(userParams)

    val scanParamTrees = xmlTree.filterChildren(_.tagName == "scan").map(_parseScanParamTree)
    scanList.setScans(scanParamTrees)

    assert(scansCount == scanParamTrees.length, "invalid scansCount")

    scanList
  }

  private def _parseScanParamTree(scanXmlTree: Tag[Singleton]): ScanParamTree = {
    val scanParamTree = new ScanParamTree(scanXmlTree.attributes.getOrElse("instrumentConfigurationRef", ""))

    val cvParams = scanXmlTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
      _parseCvParam(cvParam.attributes)
    }
    val userParams = scanXmlTree.filterChildren(_.tagName == "userParam").map { userParam =>
      _parseUserParam(userParam.attributes)
    }

    scanParamTree.setCVParams(cvParams)
    scanParamTree.setUserParams(userParams)

    val scanWindowListTreeOpt = scanXmlTree.findFirstChildNamed("scanWindowList")
    if (scanWindowListTreeOpt.isDefined) {
      val scanWindowListTree = scanWindowListTreeOpt.get
      val scanWindowsCount = scanWindowListTree.attributes.get("count").map(_.toInt).getOrElse(0)

      val scanWindowList = new ScanWindowList(scanWindowsCount)
      val scanWindows = scanWindowListTree.filterChildren(_.tagName == "scanWindow").map { scanWindowTree =>
        val scanWindow = new ScanWindow()
        scanWindow.setCVParams(
          scanWindowTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
            _parseCvParam(cvParam.attributes)
          }
        )

        scanWindow
      }
      assert(scanWindowsCount == scanWindows.length, "invalid scanWindowsCount")

      scanWindowList.setScanWindows(scanWindows)

      scanParamTree.setScanWindowList(scanWindowList)
    }

    scanParamTree
  }

  @inline
  def parsePrecursor(precursorAsStr: String): Precursor = {
    null
  }

  @inline
  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }
}


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