package com.github.mzdb4s.io.mzml

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._
import quickxml.PullParserEventType

object MzMLMetaDataParser {

  import PullParserEventType._

  /*
  Example:
  <mzML xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd" version="1.1.0" id="OVEMB150205_12">
    <cvList count="2">
      <cv id="MS" fullName="Mass spectrometry ontology" version="4.1.12" URI="https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo" />
      <cv id="UO" fullName="Unit Ontology" version="09:04:2014" URI="https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo" />
    </cvList>
    <fileDescription>
      <fileContent>
        <cvParam cvRef="MS" accession="MS:1000579" value="" name="MS1 spectrum" />
        <cvParam cvRef="MS" accession="MS:1000580" value="" name="MSn spectrum" />
      </fileContent>
      <sourceFileList count="1">
        <sourceFile id="RAW1" name="OVEMB150205_12" location=".\OVEMB150205_12.raw">
          <cvParam cvRef="MS" accession="MS:1000768" value="" name="Thermo nativeID format" />
          <cvParam cvRef="MS" accession="MS:1000563" value="" name="Thermo RAW format" />
        </sourceFile>
      </sourceFileList>
    </fileDescription>
    <referenceableParamGroupList count="1">
      <referenceableParamGroup id="commonInstrumentParams">
        <cvParam cvRef="MS" accession="MS:1001742" value="" name="LTQ Orbitrap Velos" />
        <cvParam cvRef="MS" accession="MS:1000529" value="03359B" name="instrument serial number" />
      </referenceableParamGroup>
    </referenceableParamGroupList>
    <sampleList count="1">
      <sample id="sample1" name="UPS1 5fmol R1"></sample>
    </sampleList>
    <softwareList count="1">
      <software id="ThermoRawFileParser" version="1.1.11 ">
        <cvParam cvRef="MS" accession="MS:1000799" value="ThermoRawFileParser" name="custom unreleased software tool" />
      </software>
    </softwareList>

    <scanSettingsList count="1">
      <scanSettings id="acquisition_settings_MIAPE_example">
        <sourceFileRefList count="1">
          <sourceFileRef ref="sf_parameters"/>
        </sourceFileRefList>
        <targetList count="1">
          <target>
            <cvParam cvRef="MS" accession="MS:1000744" name="selected ion m/z" value="1000" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
          </target>
        </targetList>
      </scanSettings>
    </scanSettingsList>

    <instrumentConfigurationList count="2">
      <instrumentConfiguration id="IC1">
        <referenceableParamGroupRef ref="commonInstrumentParams" />
        <componentList count="3">
          <source order="1">
            <cvParam cvRef="MS" accession="MS:1000398" value="" name="nanoelectrospray" />
          </source>
          <analyzer order="2">
            <cvParam cvRef="MS" accession="MS:1000079" value="" name="fourier transform ion cyclotron resonance mass spectrometer" />
          </analyzer>
          <detector order="3">
            <cvParam cvRef="MS" accession="MS:1000624" value="" name="inductive detector" />
          </detector>
        </componentList>
      </instrumentConfiguration>
      <instrumentConfiguration id="IC2">
        <referenceableParamGroupRef ref="commonInstrumentParams" />
        <componentList count="3">
          <source order="1">
            <cvParam cvRef="MS" accession="MS:1000398" value="" name="nanoelectrospray" />
          </source>
          <analyzer order="2">
            <cvParam cvRef="MS" accession="MS:1000264" value="" name="ion trap" />
          </analyzer>
          <detector order="3">
            <cvParam cvRef="MS" accession="MS:1000253" value="" name="electron multiplier" />
          </detector>
        </componentList>
      </instrumentConfiguration>
    </instrumentConfigurationList>
    <dataProcessingList count="1">
      <dataProcessing id="ThermoRawFileParserProcessing">
        <processingMethod order="0" softwareRef="ThermoRawFileParser">
          <cvParam cvRef="MS" accession="MS:1000544" value="" name="Conversion to mzML" />
        </processingMethod>
      </dataProcessing>
    </dataProcessingList>
    <run id="OVEMB150205_12" defaultInstrumentConfigurationRef="IC1" startTimeStamp="2015-02-05T19:30:47.537Z" defaultSourceFileRef="RAW1" sampleRef="sample1">
    </run>
  </mzML>
   */

  // TODO: parse scanSettingsList
  def parseMetaData(metaDataAsXmlString: String): MzMLMetaData = {

    val pullParser = quickxml.QuickXmlPullParser(metaDataAsXmlString)

    val cvParamsBuffer = new ArrayBuffer[CVParam]()
    val userParamsBuffer = new ArrayBuffer[UserParam]()

    val fileContent = FileContent()
    val commonInstParamTree = ParamTree()
    val commonInstrumentParams = CommonInstrumentParams(id = 1, paramTree = commonInstParamTree)

    // Source file list
    val sourceFileList = new ArrayBuffer[SourceFile]()
    val sourceFileByRef = new HashMap[String,SourceFile]

    // Sample list
    val sampleList = new ArrayBuffer[Sample]()
    val sampleByRef = new HashMap[String,Sample]

    // Software list
    val softwareList = new ArrayBuffer[Software]()
    val softwareByRef = new HashMap[String,Software]

    // Instrument config list
    val instConfigList = new ArrayBuffer[InstrumentConfiguration]()
    val instConfigByRef = new HashMap[String,InstrumentConfiguration]
    var curComponentListBuilder: ComponentListBuilder = null

    // Processing method list
    val procMethodList = new ArrayBuffer[ProcessingMethod]()
    //val procMethodByRef = new HashMap[String,ProcessingMethod]
    var curDataProcessingName: String = null

    // Run
    var runOpt = Option.empty[Run]

    var curAbstractParamTree: AbstractParamTree = null
    val attrsMap = new collection.mutable.HashMap[String,String]

    def appendItemToList[T](buildItem: ParamTree => T, itemList: ArrayBuffer[T], itemByRef: HashMap[String,T]): T = {
      pullParser.parseAttributes(attrsMap, clear = true)

      val paramTree = ParamTree()
      curAbstractParamTree = paramTree
      cvParamsBuffer.clear()
      userParamsBuffer.clear()

      val item = buildItem(paramTree)
      itemList += item
      itemByRef.put(attrsMap("id"),item)

      item
    }

    def fillAbstractParamTree(): Unit = {
      curAbstractParamTree.setCVParams(cvParamsBuffer.toList)
      cvParamsBuffer.clear()
      curAbstractParamTree.setUserParams(userParamsBuffer.toList)
      userParamsBuffer.clear()
    }

    var reachedEndOfDoc = false
    while (!reachedEndOfDoc) {

      val event = pullParser.nextEvent()

      if (event == Eof) {
        reachedEndOfDoc = true
      }
      else if (event == StartTag || event == StartEndTag) {
        val elementName = pullParser.lastElementName().getOrElse(
          throw new Exception("can't retrieve expected XML element name")
        )

        elementName match {
          case "fileContent" => {
            curAbstractParamTree = fileContent
          }
          case "sourceFile" => {
            appendItemToList(
              paramTree => SourceFile(sourceFileList.length + 1, attrsMap("name"), attrsMap("location"), paramTree),
              sourceFileList,
              sourceFileByRef
            )
          }
          case "referenceableParamGroup" => {
            pullParser.parseAttributes(attrsMap, clear = true)
            if (attrsMap("id") == "commonInstrumentParams")
              curAbstractParamTree = commonInstParamTree
          }
          case "sample" => {
            appendItemToList(
              paramTree => Sample(sampleList.length + 1, attrsMap("name"), paramTree),
              sampleList,
              sampleByRef
            )
          }
          case "software" => {
            appendItemToList(
              paramTree => Software(softwareList.length + 1, attrsMap("id"), attrsMap("version"), paramTree),
              softwareList,
              softwareByRef
            )
          }
          case "instrumentConfiguration" => {
            appendItemToList(
              paramTree => InstrumentConfiguration(instConfigList.length + 1, attrsMap("id"), None, paramTree, null),
              instConfigList,
              instConfigByRef
            )
          }
          case "softwareRef" => {
            pullParser.parseAttributes(attrsMap, clear = true)
            val softwareRef = attrsMap("ref")
            val software = softwareByRef(softwareRef)
            instConfigList.last.softwareId = Some(software.id)
          }
          case "componentList" => {
            fillAbstractParamTree() // fill instrument config param tree
            curComponentListBuilder = new ComponentListBuilder()
          }
          case "source" => {
            curAbstractParamTree = ParamTree()
          }
          case "analyzer" => {
            curAbstractParamTree = ParamTree()
          }
          case "detector" => {
            curAbstractParamTree = ParamTree()
          }
          case "dataProcessing" => {
            pullParser.parseAttributes(attrsMap, clear = true)
            curDataProcessingName = attrsMap("id")
          }
          case "processingMethod" => {
            pullParser.parseAttributes(attrsMap, clear = true)

            val paramTree = ParamTree()
            curAbstractParamTree = paramTree
            cvParamsBuffer.clear()
            userParamsBuffer.clear()

            val softRef = attrsMap("softwareRef")
            val softId = softwareByRef(softRef).id
            val procMethod = ProcessingMethod(
              procMethodList.length + 1, attrsMap("order").toInt,
              curDataProcessingName,
              Some(paramTree),
              softId
            )

            procMethodList += procMethod
          }
          case "run" => {
            // <run id="OVEMB150205_12" defaultInstrumentConfigurationRef="IC1" startTimeStamp="2015-02-05T19:30:47.537Z" defaultSourceFileRef="RAW1" sampleRef="sample1">
            pullParser.parseAttributes(attrsMap, clear = true)

            val dateAsStr = attrsMap("startTimeStamp")
            val splitDate = dateAsStr.split('.')
            val startTimestamp = if (splitDate.length == 1) {
              com.github.mzdb4s.util.date.DateParser.parseIsoDate(dateAsStr)
            } else {
              com.github.mzdb4s.util.date.DateParser.parseIsoDate(splitDate.head + "Z")
            }

            // FIXME: mzDB fields default_scan_processing_id/default_chrom_processing_id should be mapped
            //  to mzML attribute defaultDataProcessingRef of spectrumList/chromatogramList nodes
            val run = Run(
              id = 1,
              name = attrsMap("id"),
              startTimestamp = startTimestamp,
              paramTree = ParamTree(),
              instrumentConfigId = Some(instConfigByRef(attrsMap("defaultInstrumentConfigurationRef")).id),
              sampleId = attrsMap.get("sampleRef").map(ref => sampleByRef(ref).id),
              sourceFileId = attrsMap.get("defaultSourceFileRef").map(ref => sourceFileByRef(ref).id)
            )

            runOpt = Some(run)
          }
          case "cvParam" => {
            cvParamsBuffer += _parseCVParam(pullParser.parseAttributes(attrsMap, clear = true))
          }
          case "userParam" => {
            userParamsBuffer += _parseUserParam(pullParser.parseAttributes(attrsMap, clear = true))
          }
          case _ =>
        }

      } else if (event == EndTag) {
        val elementName = pullParser.lastElementName().getOrElse(
          throw new Exception("can't retrieve expected XML element name")
        )

        var fillParamTree = false
        elementName match {
          case "fileContent" =>
            fillParamTree = true
          case "sourceFile" =>
            fillParamTree = true
          case "referenceableParamGroup" =>
            if (curAbstractParamTree == commonInstParamTree)
              fillParamTree = true
          case "sample" =>
            fillParamTree = true
          case "software" =>
            fillParamTree = true
          case "source" =>
            fillAbstractParamTree()
            curComponentListBuilder.addSourceComponent(curAbstractParamTree.asInstanceOf[ParamTree])
          case "analyzer" =>
            fillAbstractParamTree()
            curComponentListBuilder.addAnalyzerComponent(curAbstractParamTree.asInstanceOf[ParamTree])
          case "detector" =>
            fillAbstractParamTree()
            curComponentListBuilder.addDetectorComponent(curAbstractParamTree.asInstanceOf[ParamTree])
          case "componentList" =>
            instConfigList.last.componentList = curComponentListBuilder.toComponentList()
          case "processingMethod" =>
            fillParamTree = true
          case _ =>
        }

        if (fillParamTree) {
          fillAbstractParamTree()
        }

      }
    }

    pullParser.dispose()

    assert(runOpt.isDefined, "can't parse 'run' tag from provided XML chunk: " + metaDataAsXmlString)

    MzMLMetaData(
      fileContent,
      commonInstrumentParams,
      instConfigList,
      procMethodList,
      Seq(runOpt.get),
      sampleList,
      softwareList,
      sourceFileList
    )
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

}
