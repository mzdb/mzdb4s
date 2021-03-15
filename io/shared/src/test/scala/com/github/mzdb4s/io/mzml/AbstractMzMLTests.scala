package com.github.mzdb4s.io.mzml

import utest._

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

trait AbstractMzMLTests extends TestSuite {

  val tests = Tests {
    'parseMetaData - parseMetaData()
  }

  def parseMetaData(): Unit = {

    val metaDataExample =
      """<mzML xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd" version="1.1.0" id="OVEMB150205_12">
        |  <cvList count="2">
        |    <cv id="MS" fullName="Mass spectrometry ontology" version="4.1.12" URI="https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo" />
        |    <cv id="UO" fullName="Unit Ontology" version="09:04:2014" URI="https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo" />
        |  </cvList>
        |  <fileDescription>
        |    <fileContent>
        |      <cvParam cvRef="MS" accession="MS:1000579" value="" name="MS1 spectrum" />
        |      <cvParam cvRef="MS" accession="MS:1000580" value="" name="MSn spectrum" />
        |    </fileContent>
        |    <sourceFileList count="1">
        |      <sourceFile id="RAW1" name="OVEMB150205_12" location=".\OVEMB150205_12.raw">
        |        <cvParam cvRef="MS" accession="MS:1000768" value="" name="Thermo nativeID format" />
        |        <cvParam cvRef="MS" accession="MS:1000563" value="" name="Thermo RAW format" />
        |      </sourceFile>
        |    </sourceFileList>
        |  </fileDescription>
        |  <referenceableParamGroupList count="1">
        |    <referenceableParamGroup id="commonInstrumentParams">
        |      <cvParam cvRef="MS" accession="MS:1001742" value="" name="LTQ Orbitrap Velos" />
        |      <cvParam cvRef="MS" accession="MS:1000529" value="03359B" name="instrument serial number" />
        |    </referenceableParamGroup>
        |  </referenceableParamGroupList>
        |  <sampleList count="1">
        |    <sample id="sample1" name="UPS1 5fmol R1"></sample>
        |  </sampleList>
        |  <softwareList count="1">
        |    <software id="ThermoRawFileParser" version="1.2.3">
        |      <cvParam cvRef="MS" accession="MS:1000799" value="ThermoRawFileParser" name="custom unreleased software tool" />
        |    </software>
        |  </softwareList>
        |
        |  <scanSettingsList count="1">
        |    <scanSettings id="acquisition_settings_MIAPE_example">
        |      <sourceFileRefList count="1">
        |        <sourceFileRef ref="sf_parameters"/>
        |      </sourceFileRefList>
        |      <targetList count="1">
        |        <target>
        |          <cvParam cvRef="MS" accession="MS:1000744" name="selected ion m/z" value="1000" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
        |        </target>
        |      </targetList>
        |    </scanSettings>
        |  </scanSettingsList>
        |
        |  <instrumentConfigurationList count="2">
        |    <instrumentConfiguration id="IC1">
        |      <referenceableParamGroupRef ref="commonInstrumentParams" />
        |      <componentList count="3">
        |        <source order="1">
        |          <cvParam cvRef="MS" accession="MS:1000398" value="" name="nanoelectrospray" />
        |        </source>
        |        <analyzer order="2">
        |          <cvParam cvRef="MS" accession="MS:1000079" value="" name="fourier transform ion cyclotron resonance mass spectrometer" />
        |        </analyzer>
        |        <detector order="3">
        |          <cvParam cvRef="MS" accession="MS:1000624" value="" name="inductive detector" />
        |        </detector>
        |      </componentList>
        |    </instrumentConfiguration>
        |    <instrumentConfiguration id="IC2">
        |      <referenceableParamGroupRef ref="commonInstrumentParams" />
        |      <componentList count="3">
        |        <source order="1">
        |          <cvParam cvRef="MS" accession="MS:1000398" value="" name="nanoelectrospray" />
        |        </source>
        |        <analyzer order="2">
        |          <cvParam cvRef="MS" accession="MS:1000264" value="" name="ion trap" />
        |        </analyzer>
        |        <detector order="3">
        |          <cvParam cvRef="MS" accession="MS:1000253" value="" name="electron multiplier" />
        |        </detector>
        |      </componentList>
        |    </instrumentConfiguration>
        |  </instrumentConfigurationList>
        |  <dataProcessingList count="1">
        |    <dataProcessing id="ThermoRawFileParserProcessing">
        |      <processingMethod order="0" softwareRef="ThermoRawFileParser">
        |        <cvParam cvRef="MS" accession="MS:1000544" value="" name="Conversion to mzML" />
        |      </processingMethod>
        |    </dataProcessing>
        |  </dataProcessingList>
        |  <run id="OVEMB150205_12" defaultInstrumentConfigurationRef="IC1" startTimeStamp="2015-02-05T19:30:47.537Z" defaultSourceFileRef="RAW1" sampleRef="sample1">
        |  </run>
        |</mzML>
        |""".stripMargin

    val parsedMetaData = MzMLMetaDataParser.parseMetaData(metaDataExample)

    // --- Check CommonInstrumentParams --- //
    parsedMetaData.commonInstrumentParams ==> CommonInstrumentParams(
      id = 1,
      paramTree = ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1001742", name = "LTQ Orbitrap Velos"),
          CVParam(accession = "MS:1000529", name = "instrument serial number", value = "03359B")
        )
      )
    )

    // --- Check instrument configurations --- //
    val compListBuilder = new ComponentListBuilder()
      .addSourceComponent(ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1000398", name = "nanoelectrospray")
        )
      ))
      .addAnalyzerComponent(ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1000484", name = "orbitrap")
        )
      ))
      .addDetectorComponent(ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1000624", name = "inductive detector")
        )
      ))

    val instrumentConfigurations = Seq(
      InstrumentConfiguration(
        id = 1,
        name = "IC1",
        paramTree = null,
        componentList = compListBuilder.toComponentList(),
        softwareId = Some(1)
      )
    )

    parsedMetaData.instrumentConfigurations.zip(instrumentConfigurations).forall { case (parsedIC, expectedIC) =>
      parsedIC.componentList.components == expectedIC.componentList.components
    }

    // --- Check processing methods --- //
    parsedMetaData.processingMethods ==> Seq(
      ProcessingMethod(
        id = 1,
        number = 0,
        dataProcessingName = "ThermoRawFileParserProcessing",
        paramTree = Some(ParamTree(
          cvParams = List(
            CVParam(accession = "MS:1000544", name = "Conversion to mzML")
          )
        )),
        softwareId = 1
      )
    )


    // --- Check runs --- //
    parsedMetaData.runs ==> Seq(Run(
      id = 1,
      name = "OVEMB150205_12",
      startTimestamp = com.github.mzdb4s.util.date.DateParser.parseIsoDate("2015-02-05T19:30:47Z"),
      paramTree = ParamTree(),
      Some(1),Some(1),Some(1)
    ))

    // --- Check samples --- //
    parsedMetaData.samples ==> Seq(Sample(id = 1, name = "UPS1 5fmol R1", paramTree = ParamTree()))

    // --- Check software list --- //
    parsedMetaData.softwareList ==> Seq(
      // FIXME: Xcalibur information missing from ThermoRawFileParser
      //Software(id = 1, name = "Xcalibur", version = "2.7.0 SP1", paramTree = null),
      Software(id = 1, name = "ThermoRawFileParser", version = "1.2.3", paramTree =  ParamTree(
        cvParams = Seq(CVParam(accession = "MS:1000799", name = "custom unreleased software tool", value = "ThermoRawFileParser"))
      ))
    )

    // --- Check source files --- //
    parsedMetaData.sourceFiles ==> Seq(
      SourceFile(
        1,"OVEMB150205_12",".\\OVEMB150205_12.raw",
        ParamTree(cvParams = List(
          CVParam(accession = "MS:1000768",name = "Thermo nativeID format"),
          CVParam(accession = "MS:1000563",name = "Thermo RAW format")
        ))
      )
    )


  }
}
