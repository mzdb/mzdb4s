/*package com.github.mzdb4s.io.reader.param

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

private[mzdb4s] object ParamTreeParserImpl {

  implicit def tag2richTag[T <: Singleton](tag: Tag[T]): JvmRichTag[T] = new JvmRichTag[T](tag)

  @inline
  def parseParamTree(paramTreeAsStr: String): ParamTree = {

    //val xmlTree = pine.XmlParser.fromString(paramTreeAsStr)
    //println("paramTreeAsStr:",paramTreeAsStr)
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

  def parseCvAndUserParams(xmlStr: String, paramTree: AbstractParamTree): Unit = {
    val xmlTree = pine.internal.HtmlParser.fromString(xmlStr, xml = true)
    parseCvAndUserParams(xmlTree, paramTree)
  }

  def parseCvAndUserParams(xmlTag: Tag[Singleton], paramTree: AbstractParamTree): Unit = {

    val cvParams = xmlTag.filterChildren(_.tagName == "cvParam").map { cvParam =>
      _parseCvParam(cvParam.attributes)
    }
    val userParams = xmlTag.filterChildren(_.tagName == "userParam").map { userParam =>
      _parseUserParam(userParam.attributes)
    }

    paramTree.setCVParams(cvParams)
    paramTree.setUserParams(userParams)
  }

  /*def parseParamTree(paramTreeAsXmlTag: Tag[Singleton]): ParamTree = {

    //val xmlTree = pine.XmlParser.fromString(paramTreeAsStr)
    //println("paramTreeAsStr:",paramTreeAsStr)
    val xmlTree = pine.internal.HtmlParser.fromString(paramTreeAsStr, xml = true)
    val cvParamsTreeOpt = xmlTree.findFirstChildNamed(rootChildName)
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
  }*/

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

    /*
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
      |""".stripMargin*/

    val xmlTree = pine.internal.HtmlParser.fromString(scanListAsStr, xml = true)

    val scansCount = xmlTree.attributes.get("count").map(_.toInt).getOrElse(0)
    val scanList = new ScanList(scansCount)

    parseCvAndUserParams(xmlTree, scanList)

    val scanParamTrees = xmlTree.filterChildren(_.tagName == "scan").map(_parseScanParamTree)
    scanList.setScans(scanParamTrees)

    assert(scansCount == scanParamTrees.length, "invalid scansCount")

    scanList
  }

  private def _parseScanParamTree(scanXmlTree: Tag[Singleton]): ScanParamTree = {
    val scanParamTree = new ScanParamTree(scanXmlTree.attributes.getOrElse("instrumentConfigurationRef", ""))

    parseCvAndUserParams(scanXmlTree, scanParamTree)

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
  def parsePrecursors(precursorListAsStr: String): Seq[Precursor] = {
    /*"""<precursorList count="1">
      |  <precursor spectrumRef="controllerType=0 controllerNumber=1 scan=2">
      |    <isolationWindow>
      |      <cvParam cvRef="MS" accession="MS:1000827" value="810.789428710938" name="isolation window target m/z" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |      <cvParam cvRef="MS" accession="MS:1000828" value="1" name="isolation window lower offset" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |      <cvParam cvRef="MS" accession="MS:1000829" value="1" name="isolation window upper offset" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |    </isolationWindow>
      |    <selectedIonList count="1">
      |      <selectedIon>
      |        <cvParam cvRef="MS" accession="MS:1000744" value="810.789428710938" name="selected ion m/z" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      |      </selectedIon>
      |    </selectedIonList>
      |    <activation>
      |      <cvParam cvRef="MS" accession="MS:1000045" value="35" name="collision energy" unitAccession="UO:0000266" unitName="electronvolt" unitCvRef="UO" />
      |      <cvParam cvRef="MS" accession="MS:1000133" value="" name="collision-induced dissociation" />
      |    </activation>
      |  </precursor>
      |</precursorList>
      |""".stripMargin*/

    val xmlTree = pine.internal.HtmlParser.fromString(precursorListAsStr, xml = true)

    val precursorsCount = xmlTree.attributes.get("count").map(_.toInt).getOrElse(0)

    // Handle chunk with or without <precursorList count="1">
    val precursorXmlTrees = if (precursorsCount == 0) List(xmlTree) else xmlTree.filterChildren(_.tagName == "precursor")

    val precursors = precursorXmlTrees.map { precursorXmlTree: Tag[Singleton] =>

      val prec = new Precursor(precursorXmlTree.attributes.getOrElse("spectrumRef", ""))

      parseCvAndUserParams(precursorXmlTree, prec)

      // --- Parse activation type --- //
      val activationOpt = precursorXmlTree.findFirstChildNamed("activation")
      if (activationOpt.isDefined) {
        val activationTree = activationOpt.get

        val activation = new Activation()

        activation.setCVParams(
          activationTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
            _parseCvParam(cvParam.attributes)
          }
        )

        prec.setActivation(activation)
      }

      // --- Parse isolation window --- //
      val isolationWindowOpt = precursorXmlTree.findFirstChildNamed("isolationWindow")
      if (isolationWindowOpt.isDefined) {
        val isolationWindowTree = isolationWindowOpt.get

        val isolationWindow = new IsolationWindowParamTree()

        isolationWindow.setCVParams(
          isolationWindowTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
            _parseCvParam(cvParam.attributes)
          }
        )

        prec.setIsolationWindow(isolationWindow)
      }

      // --- Parse selected ions --- //
      val selectedIonListTreeOpt = precursorXmlTree.findFirstChildNamed("selectedIonList")
      if (selectedIonListTreeOpt.isDefined) {
        val selectedIonListTree = selectedIonListTreeOpt.get
        val selectedIonsCount = selectedIonListTree.attributes.get("count").map(_.toInt).getOrElse(0)

        val selectedIonList = new SelectedIonList(selectedIonsCount)
        val selectedIons = selectedIonListTree.filterChildren(_.tagName == "selectedIon").map { selectedIonTree =>
          val selectedIon = new SelectedIon()
          selectedIon.setCVParams(
            selectedIonTree.filterChildren(_.tagName == "cvParam").map { cvParam =>
              _parseCvParam(cvParam.attributes)
            }
          )

          selectedIon
        }
        assert(selectedIonsCount == selectedIons.length, "invalid selectedIonsCount")

        selectedIonList.setSelectedIons(selectedIons)

        prec.setSelectedIonList(selectedIonList)
      }

      prec
    }

    //assert(precursorsCount == precursors.length, "invalid precursorsCount")

    precursors
  }

  @inline
  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }

}*/

