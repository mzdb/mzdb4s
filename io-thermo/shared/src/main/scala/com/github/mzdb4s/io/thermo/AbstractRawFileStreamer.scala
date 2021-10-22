package com.github.mzdb4s.io.thermo

import java.io.File

import com.github.mzdb4s.Logging
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params.param._
import com.github.mzdb4s.io.mzml._
import com.github.mzdb4s.io.reader.param._
import com.github.mzdb4s.msdata.{ActivationType, SpectrumHeader}

abstract class AbstractRawFileStreamer private[thermo](rawFilePath: File) extends Logging {

  require(rawFilePath.isFile, s"can't find file at '$rawFilePath'")

  protected val MS_LEVEL_ACCESSION: String = PsiMsCV.MS_LEVEL.getAccession()
  protected val CID_ACCESSION: String = PsiMsCV.CID.getAccession()
  protected val ETD_ACCESSION: String = PsiMsCV.ETD.getAccession()
  protected val ETHCD_ACCESSION: String = PsiMsCV.ETHCD.getAccession()
  protected val HCD_ACCESSION: String = PsiMsCV.HCD.getAccession()
  protected val PSD_ACCESSION: String = PsiMsCV.PSD.getAccession()
  protected var _isConsumed = false

  protected val paramTreeParser: IParamTreeParser

  def forEachSpectrum( onEachSpectrum: RawFileSpectrum => Boolean ): Unit

  def getMetaDataAsXmlString(): String

  def getMetaData(converterVersion: String): MzMLMetaData = {

    // FIXME: the .Net library should return a correct XML chunk
    val xmlString = getMetaDataAsXmlString() + "/></mzML>"

    val parsedMetaData = MzMLMetaDataParser.parseMetaData(xmlString)

    val softList = parsedMetaData.softwareList
    val thermoRawFileParserId = softList.find(_.name == "ThermoRawFileParser").map(_.id).getOrElse(
      throw new Exception("can't find 'ThermoRawFileParser' software entry in XML meta-data:\n" + xmlString)
    )
    val newSoftId = softList.last.id + 1

    parsedMetaData.copy(
      softwareList = softList ++ Seq(
        Software(id = newSoftId, name = "Thermo2mzDB", version = converterVersion, paramTree = null)
      ),
      processingMethods = Seq(
        ProcessingMethod(
          id = 1,
          number = 1,
          dataProcessingName = "ThermoRawFileParser mzML streaming",
          paramTree = Some(ParamTree(
            cvParams = List(
              CVParam(accession = "MS:1000544", name = "Conversion to mzML")
            )
          )),
          softwareId = thermoRawFileParserId
        ),
        ProcessingMethod(
          id = 2,
          number = 2,
          dataProcessingName = "mzML to mzDB conversion",
          paramTree = Some(ParamTree(
            userParams = List(
              UserParam(name = "Conversion to mzDB", value = "", `type` = "xsd:string")
            )
          )),
          softwareId = newSoftId
        )
      )
    )
  }

  /*private def _parseFileDescription(xmlString: String): (FileContent,Seq[SourceFile]) = {
    import ParamTreeParserImpl.tag2richTag

    val xmlTree = pine.internal.HtmlParser.fromString(xmlString, xml = true)

    val fileDescTreeOpt = xmlTree.findFirstChildNamed("fileDescription")
    val fileContentTreeOpt = fileDescTreeOpt.flatMap(_.findFirstChildNamed("fileContent"))
    val sourceFileListTreeOpt = fileDescTreeOpt.flatMap(_.findFirstChildNamed("sourceFileList"))

    // Parse File Content
    val fileContent = FileContent()
    if (fileContentTreeOpt.isDefined) {
      ParamTreeParserImpl.parseCvAndUserParams(fileContentTreeOpt.get, fileContent)
    }

    // Parse source file list
    val sourceFiles = if (sourceFileListTreeOpt.isEmpty) Seq.empty[SourceFile]
    else {
      var fileId = 0
      sourceFileListTreeOpt.get.filterChildren(_.tagName == "sourceFile").map { sourceFileTree =>
        fileId += 1

        val sourceFileAttrs = sourceFileTree.attributes
        val fileName = sourceFileAttrs.getOrElse("name", rawFilePath.getName)
        val fileLocation = sourceFileAttrs.getOrElse("location", "file:///" + rawFilePath.getCanonicalFile.getAbsolutePath)

        val paramTree = new ParamTree()
        ParamTreeParserImpl.parseCvAndUserParams(sourceFileTree, paramTree)

        SourceFile(
          id = fileId,
          name = fileName,
          location = fileLocation,
          paramTree = paramTree
        )
      }
    }
  }*/

/*
  // FIXME
  private def _parseFileDescription(xmlString: String): (FileContent,Seq[SourceFile]) = {
    val fileContent = FileContent()
    val sourceFiles = Seq.empty[SourceFile]
    (fileContent, sourceFiles)
  }

  def getMetaData(converterVersion: String): MzMLMetaData = {

    // FIXME: the .Net library should return a correct XML chunk
    val xmlString = getMetaDataAsXmlString() + "/></mzML>"

    val (fileContent, sourceFiles) = _parseFileDescription(xmlString)

    // FIXME: parse this information
    val commonInstrumentParams = CommonInstrumentParams(
      id = 1,
      paramTree = ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1001742", name = "LTQ Orbitrap Velos"),
          CVParam(accession = "MS:1000529", name = "instrument serial number", value = "03359B")
        )
      )
    )

    // FIXME: parse this information
    val compListBuilder = new ComponentListBuilder()
      .addSourceComponent(ParamTree(
        cvParams = List(
          CVParam(accession = "MS:1000398", name = "nanoelectrospray"),
          CVParam(accession = "MS:1000485", name = "nanospray")
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

    val processingMethods = Seq(
      ProcessingMethod(
        id = 1,
        number = 1,
        dataProcessingName = "ThermoRawFileParser mzML streaming",
        paramTree = Some(ParamTree(
          cvParams = List(
            CVParam(accession = "MS:1000544", name = "Conversion to mzML")
          )
        )),
        softwareId = 1
      ),
      ProcessingMethod(
        id = 2,
        number = 2,
        dataProcessingName = "mzML to mzDB conversion",
        paramTree = Some(ParamTree(
          userParams = List(
            UserParam(name = "Conversion to mzDB", value = "", `type` = "xsd:string")
          )
        )),
        softwareId = 1
      )
    )

    // TODO: static
    //val df1 = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val runs = Seq(Run(
      id = 1,
      name = "OVEMB150205_12",
      // FIXME: retrieve the correct creation date
      startTimestamp = new java.util.Date(),//df1.parse("2015-02-05T19:30:47Z"), //java.time.Instant.parse("2015-02-05T19:30:47Z"),
      paramTree = null
    ))

    val samples = Seq(Sample(id = 1, name = "UPS1 5fmol R1", paramTree = null))

    val softwareList = Seq(
      // FIXME: Xcalibur information missing from ThermoRawFileParser
      //Software(id = 1, name = "Xcalibur", version = "2.7.0 SP1", paramTree = null),
      Software(id = 1, name = "ThermoRawFileParser", version = "1.2.3", paramTree = null),
      Software(id = 2, name = "Thermo2mzDB", version = converterVersion, paramTree = null)
    )

    MzMLMetaData(
      fileContent,
      commonInstrumentParams,
      instrumentConfigurations,
      processingMethods,
      runs,
      samples,
      softwareList,
      sourceFiles
    )
  }*/

  protected def parseSpectrumXmlMetaData(spectrumId: Int, xmlStr: String): (SpectrumXmlMetaData, String) = {

    var lineIdx = 0
    var spectrumTitle = ""
    val paramTreeCvParamsBuilder = new StringBuilder()
    val scanListBuilder = new StringBuilder()
    val precursorListBuilder = new StringBuilder()
    var curStringBuilder = paramTreeCvParamsBuilder
    var linePrefix = "  "

    for (line <- xmlStr.split('\n').toList) {
      if (lineIdx == 0) {
        spectrumTitle = line.split('"')(1)
      }
      else {
        if (line.contains("<scanList")) {
          linePrefix = null
          curStringBuilder = scanListBuilder
        }
        if (line.contains("<precursorList")) curStringBuilder = precursorListBuilder

        if (!line.contains("</spectrum>")) {
          if (linePrefix == null)
            curStringBuilder ++= line
          else
            curStringBuilder ++= s"$linePrefix$line"

          curStringBuilder += '\n'
        }
      }

      lineIdx += 1
    }

    /*println(paramTreeCvParamsBuilder.result())
    println("****")
    println(scanListBuilder.result())
    println("****")
    println(precursorListBuilder.result())*/

    val precListAsXmlStr = precursorListBuilder.result()

    // Patch precursor list to match the mzDB specs
    // FIXME: change the mzDB specs???
    val precListOpt = if (precListAsXmlStr.isEmpty) None
    else {
      val patchedXmlStr = precListAsXmlStr
        .replaceAllLiterally("  <precursorList count=\"1\">\n","")
        .replaceAllLiterally("  </precursorList>\n","")

      Some(patchedXmlStr)
    }

    val xmlMetaData = SpectrumXmlMetaData(
      spectrumId = spectrumId,
      paramTree =
        s"""<params>
           |  <cvParams>
           |$paramTreeCvParamsBuilder  </cvParams>
           |</params>
           |""".stripMargin,
      scanList = scanListBuilder.result(),
      precursorList = precListOpt,
      productList = None
    )

    (xmlMetaData, spectrumTitle)
  }

  protected def getMsLevel(xmlMetaData: SpectrumXmlMetaData): Int = {
    val paramTree = this.paramTreeParser.parseParamTree(xmlMetaData.paramTree)
    val cvParams = paramTree.getCVParams()

    val defaultMsLevel = if (xmlMetaData.precursorList.isEmpty) 1 else 2
    cvParams.find(_.accession == MS_LEVEL_ACCESSION).map(_.value.toInt).getOrElse(defaultMsLevel)
  }

  protected def createSpectrum(
    id: Long,
    intitialId: Int,
    msLevel: Int,
    msCycle: Int,
    title: String,
    xmlMetaData: SpectrumXmlMetaData,
    mzList: Array[Double],
    intensityList: Array[Double]
  ): RawFileSpectrum = {
    //println("createSpectrum begins")

    val scanListParamTree = this.paramTreeParser.parseScanList(xmlMetaData.scanList)

    val firstScan = scanListParamTree.getScans().head
    val scanStartTimeCvTerm = firstScan.getCVParam(PsiMsCV.SCAN_START_TIME)
    val scanStartTimeUnit = scanStartTimeCvTerm.unitName.getOrElse("")
    assert(scanStartTimeUnit == "minute", s"unsupported scan time unit '$scanStartTimeUnit'")
    val scanTime = scanStartTimeCvTerm.value.toFloat * 60

    var activationType = ActivationType.OTHER // unkown applied dissociation
    var precOpt = Option.empty[Precursor]
    var precMzOpt = Option.empty[Double]
    var precChargeOpt = Option.empty[Int]
    if (msLevel > 1 && xmlMetaData.precursorList.isDefined) {
      val prec = this.paramTreeParser.parsePrecursors(xmlMetaData.precursorList.get).head
      precOpt = Some(prec)

      val selIonList = prec.getSelectedIonList()
      val firstSelIonCVParams = selIonList.getSelectedIons().head.getCVParams()
      precChargeOpt = firstSelIonCVParams.find(_.getAccession == PsiMsCV.CHARGE_STATE.getAccession()).map(_.getValue.toInt)

      val activationCvParams = prec.getActivation().getCVParams()
      activationCvParams.find { cvParam =>
        cvParam.getAccession match {
          case CID_ACCESSION => activationType = ActivationType.CID; true
          case ETD_ACCESSION => activationType = ActivationType.ETD; true
          case ETHCD_ACCESSION => activationType = ActivationType.EThcD; true
          case HCD_ACCESSION => activationType = ActivationType.HCD; true
          case PSD_ACCESSION => activationType = ActivationType.PSD; true
          case _ => false
        }
      }

      val monoMzOpt = firstScan.getUserParams().find { userParam =>
        userParam.getName == "[Thermo Trailer Extra]Monoisotopic M/Z:"
      }

      precMzOpt = if (monoMzOpt.isDefined) monoMzOpt.map(_.getValue.toDouble)
      else {
        firstSelIonCVParams.find(_.getAccession == PsiMsCV.SELECTED_ION_MZ.getAccession()).map(_.getValue.toDouble)
      }
    }

    val peaksCount = mzList.length
    val intensityListAsFloats = new Array[Float](peaksCount)

    var basePeakIdx = -1
    var maxIntensity = 0f
    var intensitySum = 0f

    var i = 0
    while (i < peaksCount) {
      val intensity = intensityList(i).toFloat
      intensityListAsFloats(i) = intensity
      intensitySum += intensity

      if (intensity > maxIntensity) {
        maxIntensity = intensity
        basePeakIdx = i
      }

      i += 1
    }

    val basePeakIntensity = maxIntensity
    val basePeakMz = if (basePeakIdx > -1) mzList(basePeakIdx) else 0.0

    val header = SpectrumHeader(
      id = id,
      initialId = intitialId,
      title = title,
      cycle = msCycle,
      time = scanTime,
      msLevel = msLevel,
      activationType = if (msLevel == 1) None else Some(activationType),
      peaksCount = peaksCount,
      isHighResolution = true,
      tic = intensitySum,
      basePeakMz = basePeakMz,
      basePeakIntensity = basePeakIntensity,
      precursorMz = precMzOpt,
      precursorCharge = precChargeOpt,
      bbFirstSpectrumId = id,
      isolationWindow = None // FIXME: retrieve me
    )

    // Attach CV params to the scan header
    header.setScanList(scanListParamTree)
    precOpt.foreach(header.setPrecursor)
    //println("createSpectrum ends")

    RawFileSpectrum(intitialId, xmlMetaData, header, mzList, intensityListAsFloats)
  }

}

case class RawFileSpectrum(
  initialId: Int,
  xmlMetaData: SpectrumXmlMetaData,
  header: SpectrumHeader,
  mzList: Array[Double],
  intensityList: Array[Float]
)
