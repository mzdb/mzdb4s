package com.github.mzdb4s.io.reader.param

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.{Attribute, StartElement}

import scala.collection.Seq
import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

// See:
// - https://stackoverflow.com/questions/12269504/fastest-and-optimized-way-to-read-the-xml
// - https://stackoverflow.com/questions/41338521/xmleventreader-stax-api-not-able-to-getname-and-value-for-attribute-in-jdk-1-8
// - https://www.vogella.com/tutorials/JavaXML/article.html#javastax_read
// FIXME: private
object JVMParamTreeParserImpl {
//  private[mzdb4s] object JVMParamTreeParserImpl {

  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    require(paramTreeAsStr != null, "paramTreeAsStr is null")

    val in = new java.io.ByteArrayInputStream(paramTreeAsStr.getBytes())
    val inputFactory = XMLInputFactory.newInstance
    val eventReader = inputFactory.createXMLEventReader(in,"ISO-8859-1")

    val cvParamsBuffer = new ArrayBuffer[CVParam]()
    val userParamsBuffer = new ArrayBuffer[UserParam]()
    val userTextsBuffer = new ArrayBuffer[UserText]()
    var curUserTextAttrs: collection.Map[String,String] = null
    val curTextBuilder = new StringBuilder()

    while (eventReader.hasNext) {
      val event = eventReader.nextEvent

      if (event.isStartElement) {
        val startElem = event.asStartElement
        val elementName = startElem.getName.getLocalPart

        elementName match {
          case "cvParam" => {
            cvParamsBuffer += _parseCVParam(_startElemToAttributes(startElem))
          }
          case "userParam" => {
            userParamsBuffer += _parseUserParam(_startElemToAttributes(startElem))
          }
          case "userText" => {
            curUserTextAttrs = _startElemToAttributes(startElem)
          }
          case _ =>
        }
        // if we are currently inside a userText node
      } else if (curUserTextAttrs != null && event.isCharacters) {
        val text = event.asCharacters().getData
        curTextBuilder ++= text
      } else if (event.isEndElement) {
        val endElem = event.asEndElement()
        val elementName = endElem.getName.getLocalPart

        elementName match {
          case "userText" => {
            userTextsBuffer += _parseUserText(curUserTextAttrs, curTextBuilder.result())
            curTextBuilder.clear()
            curUserTextAttrs = null
          }
          case _ =>
        }
      }
    }

    val paramTree = new ParamTree()
    paramTree.setCVParams(cvParamsBuffer)
    paramTree.setUserParams(userParamsBuffer)
    paramTree.setUserTexts(userTextsBuffer)

    paramTree
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    require(scanListAsStr != null, "scanListAsStr is null")

    val in = new java.io.ByteArrayInputStream(scanListAsStr.getBytes())
    val inputFactory = XMLInputFactory.newInstance
    val eventReader = inputFactory.createXMLEventReader(in)

    var scanList: ScanList = null
    val scans = new ArrayBuffer[ScanParamTree]()

    var curScanParamTree: ScanParamTree = null
    var curScanWindowList: ScanWindowList = null
    var curScanWindow: ScanWindow = null

    val scanWinBuffer = new ArrayBuffer[ScanWindow]()
    val cvParamsBuffer = new ArrayBuffer[CVParam]()
    val userParamsBuffer = new ArrayBuffer[UserParam]()

    def fillAbstractParamTree(paramTree: AbstractParamTree): Unit = {
      paramTree.setCVParams(cvParamsBuffer.toList)
      cvParamsBuffer.clear()
      paramTree.setUserParams(userParamsBuffer.toList)
      userParamsBuffer.clear()
    }

    while (eventReader.hasNext) {
      val event = eventReader.nextEvent

      if (event.isStartElement) {
        val startElem = event.asStartElement
        val elementName = startElem.getName.getLocalPart

        elementName match {
          case "scanList" => {
            val attrsMap = _startElemToAttributes(startElem)
            scanList = new ScanList(attrsMap("count").toInt)
            scanList.setScans(scans)
          }
          case "scan" => {
            fillAbstractParamTree(scanList)

            val attrsMap = _startElemToAttributes(startElem)
            curScanParamTree = new ScanParamTree(attrsMap.getOrElse("instrumentConfigurationRef", ""))
            scans += curScanParamTree
          }
          case "scanWindowList" => {
            fillAbstractParamTree(curScanParamTree)

            val attrsMap = _startElemToAttributes(startElem)
            curScanWindowList = new ScanWindowList(attrsMap("count").toInt)
            curScanParamTree.setScanWindowList(curScanWindowList)
          }
          case "scanWindow" => {
            curScanWindow = new ScanWindow()
            scanWinBuffer += curScanWindow
          }
          case "cvParam" => {
            cvParamsBuffer += _parseCVParam(_startElemToAttributes(startElem))
          }
          case "userParam" => {
            userParamsBuffer += _parseUserParam(_startElemToAttributes(startElem))
          }
          case _ =>
        }


      } else if (event.isEndElement) {
        val endElem = event.asEndElement()
        val elementName = endElem.getName.getLocalPart

        elementName match {
          case "scanWindow" => {
            fillAbstractParamTree(curScanWindow)
          }
          case "scanWindowList" => {
            curScanWindowList.setScanWindows(scanWinBuffer.toList)
            scanWinBuffer.clear()
          }
          case _ =>
        }
      }
    }

    scanList
  }

  def parsePrecursors(precursorListAsStr: String): Seq[Precursor] = {
    require(precursorListAsStr != null, "precursorListAsStr is null")

    val in = new java.io.ByteArrayInputStream(precursorListAsStr.getBytes())
    val inputFactory = XMLInputFactory.newInstance
    val eventReader = inputFactory.createXMLEventReader(in)

    val precursorList = new ArrayBuffer[Precursor]()
    var expectedPrecursorsCount = -1
    var expectedSelectedIonListCount = -1

    var curPrec: Precursor = null
    var curActivation: Activation = null
    var curIsoWin: IsolationWindowParamTree = null
    var curSelectedIonList: SelectedIonList = null
    var curSelectedIon: SelectedIon = null

    val selIonBuffer = new ArrayBuffer[SelectedIon]()
    val cvParamsBuffer = new ArrayBuffer[CVParam]()
    val userParamsBuffer = new ArrayBuffer[UserParam]()

    while (eventReader.hasNext) {
      val event = eventReader.nextEvent

      if (event.isStartElement) {
        val startElem = event.asStartElement
        val elementName = startElem.getName.getLocalPart

        elementName match {
          case "precursorList" => {
            val attrsMap = _startElemToAttributes(startElem)
            expectedPrecursorsCount = attrsMap("count").toInt
          }
          case "precursor" => {
            val attrsMap = _startElemToAttributes(startElem)
            curPrec = new Precursor(attrsMap.getOrElse("spectrumRef", ""))
            precursorList += curPrec
          }
          case "isolationWindow" => {
            curIsoWin = new IsolationWindowParamTree()
            curPrec.setIsolationWindow(curIsoWin)
          }
          case "selectedIonList" => {
            val attrsMap = _startElemToAttributes(startElem)
            expectedSelectedIonListCount = attrsMap("count").toInt
            curSelectedIonList = new SelectedIonList(expectedSelectedIonListCount)
            curPrec.setSelectedIonList(curSelectedIonList)
          }
          case "selectedIon" => {
            curSelectedIon = new SelectedIon()
            selIonBuffer += curSelectedIon
          }
          case "activation" => {
            curActivation = new Activation()
            curPrec.setActivation(curActivation)
          }
          case "cvParam" => {
            cvParamsBuffer += _parseCVParam(_startElemToAttributes(startElem))
          }
          case "userParam" => {
            userParamsBuffer += _parseUserParam(_startElemToAttributes(startElem))
          }
          case _ =>
        }


      } else if (event.isEndElement) {
        val endElem = event.asEndElement()
        val elementName = endElem.getName.getLocalPart

        var curAbstractParamTree: AbstractParamTree = null
        var fillParamTree = false
        elementName match {
          case "isolationWindow" => {
            curAbstractParamTree = curIsoWin
            fillParamTree = true
          }
          case "selectedIonList" => {
            curSelectedIonList.setSelectedIons(selIonBuffer.toList)
            selIonBuffer.clear()
          }
          case "selectedIon" => {
            curAbstractParamTree = curSelectedIon
            fillParamTree = true
          }
          case "activation" => {
            curAbstractParamTree = curActivation
            fillParamTree = true
          }
          case _ =>
        }

        if (fillParamTree) {
          curAbstractParamTree.setCVParams(cvParamsBuffer.toList)
          cvParamsBuffer.clear()
          curAbstractParamTree.setUserParams(userParamsBuffer.toList)
          userParamsBuffer.clear()
        }

      }
    }

    precursorList
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }

  def parseFileContent(fileContentAsStr: String): FileContent = {
    /*"""<fileContent>
      |  <cvParam cvRef="MS" accession="MS:1000579" value="" name="MS1 spectrum" />
      |  <cvParam cvRef="MS" accession="MS:1000580" value="" name="MSn spectrum" />
      |</fileContent>""".stripMargin*/

    // Note: we reuse here our JVM implem of parseParamTree, since it also supports flat lists of cvParam/userParam
    new FileContent(this.parseParamTree(fileContentAsStr))
  }

  def _startElemToAttributes(startElem: StartElement): collection.Map[String, String] = {
    val attrsMap = new collection.mutable.HashMap[String, String]
    val iterator = startElem.getAttributes.asInstanceOf[java.util.Iterator[Attribute]]
    while (iterator.hasNext) {
      val attribute = iterator.next()
      val value = attribute.getValue
      val name = attribute.getName.toString
      //System.out.println("\t" + name + " " + value)
      attrsMap.put(name, value)
    }

    attrsMap
  }

  private def _parseCVParam(attributes: collection.Map[String,String]): CVParam = {
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


}