package com.github.mzdb4s.io.reader.param

import scala.collection.Seq
import scala.scalanative.libc.string.strstr
import scala.scalanative.libc.string.strlen
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._
import com.github.sqlite4s.c.util.CUtils

// FIXME: keep private
object NativeParamTreeParserImpl {
//private[mzdb4s] object NativeParamTreeParserImpl {

  private val cvParamsCStr = c"<cvParams>"
  private val cvParamsCStrLen: CInt = strlen(cvParamsCStr).toInt
  private val cvParamCStr = c"<cvParam "
  private val cvParamCStrLen: CInt = strlen(cvParamCStr).toInt

  private val userParamsCStr = c"<userParams>"
  private val userParamsCStrLen: CInt = strlen(userParamsCStr).toInt
  private val userParamCStr = c"<userParam "
  private val userParamCStrLen: CInt = strlen(userParamCStr).toInt

  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    require(paramTreeAsStr != null, "paramTreeAsStr is null")

    val paramTree = new ParamTree()

    Zone { implicit z =>
      //val paramTreeLength = paramTreeAsStr.length
      val paramTreeAsCStr = toCString(paramTreeAsStr)

      var cvParamsXmlChunkAsCStr = paramTreeAsCStr
      cvParamsXmlChunkAsCStr = strstr(cvParamsXmlChunkAsCStr, cvParamsCStr)

      if (cvParamsXmlChunkAsCStr != null) {
        cvParamsXmlChunkAsCStr += cvParamsCStrLen // skip '<cvParams' tag
        val offset = (cvParamsXmlChunkAsCStr - paramTreeAsCStr).toInt
        val cvParams = _parseCvParams(cvParamsXmlChunkAsCStr, paramTreeAsStr.length - offset)
        if (cvParams.nonEmpty) paramTree.setCVParams(cvParams)
      }

      var userParamsXmlChunkAsCStr = paramTreeAsCStr
      userParamsXmlChunkAsCStr = strstr(userParamsXmlChunkAsCStr, userParamsCStr)

      if (userParamsXmlChunkAsCStr != null) {
        userParamsXmlChunkAsCStr += userParamsCStrLen // skip '<userParams' tag

        val offset = (userParamsXmlChunkAsCStr - paramTreeAsCStr).toInt
        val userParams = _parseUserParams(userParamsXmlChunkAsCStr, paramTreeAsStr.length - offset)
        if (userParams.nonEmpty) paramTree.setUserParams(userParams)
      }

      // FIXME: parse userTexts
    }

    paramTree
  }

  private def _parseCvParams(xmlChunkAsCStr: CString, maxLength: Int): Seq[CVParam] = {
    val cvParams = new collection.mutable.ArrayBuffer[CVParam]()

    var tmpXmlChunk = xmlChunkAsCStr

    var parsedCvParams = false
    while (!parsedCvParams) {
      tmpXmlChunk = strstr(tmpXmlChunk, cvParamCStr)

      val tmpChunkOffset = if (tmpXmlChunk == null) maxLength else (tmpXmlChunk - xmlChunkAsCStr).toInt
      if (tmpChunkOffset >= maxLength) parsedCvParams = true
      else {
        //val chunkLen = (tmpXmlChunk - xmlChunkAsCStr).toInt + cvParamCStrLen
        val chunkLen = maxLength - tmpChunkOffset
        val cvParamAC = _parseCvParamAccession(tmpXmlChunk, chunkLen)
        val cvParamValue = _parseParamValue(tmpXmlChunk, chunkLen)
        val cvParamName = _parseParamName(tmpXmlChunk, chunkLen)
        val cvParamUnitACOpt = Option(_parseCvParamUnitAccession(tmpXmlChunk, chunkLen))
        val cvParamUnitNameOpt = Option(_parseCvParamUnitName(tmpXmlChunk, chunkLen))
        //println(s"cvParamAC='$cvParamAC' cvParamValue='$cvParamValue' cvParamName='$cvParamName' $cvParamUnitACOpt $cvParamUnitNameOpt")

        cvParams += CVParam(
          accession = cvParamAC,
          name = cvParamName,
          value = cvParamValue,
          cvRef = cvParamAC.takeWhile(_ != ':'),
          unitCvRef = cvParamUnitACOpt.map(_.takeWhile(_ != ':')),
          unitAccession = cvParamUnitACOpt,
          unitName = cvParamUnitNameOpt
        )
        //println(cvParams.last.toXml())

        tmpXmlChunk = tmpXmlChunk + cvParamCStrLen // skip '<cvParam' tag to match the next one
      }
    }

    cvParams
  }

  private def _parseUserParams(xmlChunkAsCStr: CString, maxLength: Int): Seq[UserParam] = {
    val userParams = new collection.mutable.ArrayBuffer[UserParam]()

    var tmpXmlChunk = xmlChunkAsCStr

    var parsedUserParams = false
    while (!parsedUserParams) {
      tmpXmlChunk = strstr(tmpXmlChunk, userParamCStr)

      val tmpChunkOffset = if (tmpXmlChunk == null) maxLength else (tmpXmlChunk - xmlChunkAsCStr).toInt
      if (tmpChunkOffset >= maxLength) parsedUserParams = true
      else {
        //val chunkLen = (tmpXmlChunk - paramTreeAsCStr).toInt + userParamCStrLen
        val chunkLen = maxLength - tmpChunkOffset
        val userParamName = _parseParamName(tmpXmlChunk, chunkLen)
        val userParamValue = _parseParamValue(tmpXmlChunk, chunkLen)
        val userParamType = _parseUserParamType(tmpXmlChunk, chunkLen)

        userParams += UserParam(
          name = userParamName,
          value = userParamValue,
          `type` = userParamType
        )
        //println(userParams.last.toXml())

        tmpXmlChunk = tmpXmlChunk + userParamCStrLen // skip '<userParam' tag to match the next one
      }
    }

    userParams
  }

  private val accessionCStr = c"""accession="""
  private val accessionStrLen = 10

  private def _parseCvParamAccession(cvParamChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(cvParamChunk, chunkLength, accessionCStr, accessionStrLen, chunkLength - accessionStrLen)
  }

  private val unitAccessionCStr = c"""unitAccession="""
  private val unitAccessionStrLen = strlen(unitAccessionCStr).toInt

  private def _parseCvParamUnitAccession(cvParamChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(cvParamChunk, chunkLength, unitAccessionCStr, unitAccessionStrLen, chunkLength - unitAccessionStrLen)
  }

  private val unitNameCStr = c"""unitName="""
  private val unitNameStrLen = strlen(unitNameCStr).toInt

  private def _parseCvParamUnitName(paramChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(paramChunk, chunkLength, unitNameCStr, unitNameStrLen, chunkLength - unitNameStrLen)
  }

  private val nameCStr = c"""name="""
  private val nameStrLen = strlen(nameCStr).toInt

  private def _parseParamName(paramChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(paramChunk, chunkLength, nameCStr, nameStrLen, chunkLength - nameStrLen)
  }

  private val valueCStr = c"""value="""
  private val valueStrLen = strlen(valueCStr).toInt

  private def _parseParamValue(paramChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(paramChunk, chunkLength, valueCStr, valueStrLen, chunkLength - valueStrLen)
  }

  private val typeCStr = c"""type="""
  private val typeStrLen = strlen(typeCStr).toInt

  private def _parseUserParamType(paramChunk: CString, chunkLength: CInt): String = {
    _parseAttributeContent(paramChunk, chunkLength, typeCStr, typeStrLen, chunkLength - typeStrLen)
  }

  private[mzdb4s] def _parseAttributeContent(
    xmlChunk: CString,
    chunkLen: CInt,
    attr: CString,
    attrLen: Int,
    bufferSize: Int
  ): String = {
    require(chunkLen > 0, "chunkLength must be greater than zero")
    require(bufferSize > 0, "bufferSize must be greater than zero")

    val attributeChunk = strstr(xmlChunk, attr)
    if (attributeChunk == null || (attributeChunk - xmlChunk) > chunkLen) return null
    else {
      val nextTag = strstr(attributeChunk + attrLen, c"<")
      if (nextTag != null && attributeChunk - nextTag > 0) return null
    }
    /*assert(
      attributeChunk != null,
      s"can't find '${fromCString(attr)}' attribute in XML chunk: ${fromCString(xmlChunk).take(200)}"
    )*/

    var j = 0
    var i = attrLen + 1 // skip attribute string + one char (")
    var reachedEndOfContent = false
    val attributeContentAsCStr: CString = stackalloc[CChar](bufferSize.toULong)

    while (i < chunkLen && !reachedEndOfContent) {
      val c = attributeChunk(i)
      if (c == '"' || c == CUtils.NULL_CHAR) {
        reachedEndOfContent = true
      } else {
        attributeContentAsCStr(j) = c
      }

      i += 1
      j += 1
    }

    attributeContentAsCStr(j) = CUtils.NULL_CHAR

    //println(fromCString(attr) + fromCString(attributeContentAsCStr))

    fromCString(attributeContentAsCStr)
  }

  private val scanCStr = c"<scan "
  private val scanWindowListCStr = c"<scanWindowList "
  private val scanWindowCStr = c"<scanWindow>"

  private val countCStr = c"""count="""
  private val countStrLen = strlen(countCStr).toInt

  private val instrumentConfRefCStr = c"""instrumentConfigurationRef="""
  private val instrumentConfRefStrLen = strlen(instrumentConfRefCStr).toInt

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

    Zone { implicit z =>
      val scanListAsCStr = toCString(scanListAsStr)

      val scansCount = _parseAttributeContent(scanListAsCStr, 22, countCStr, countStrLen, 4)
      assert(scansCount == "1", "parsing of multiple scans is not yet implemented (please open an issue on github)")

      val scanXmlChunk = strstr(scanListAsCStr, scanCStr)
      assert(scanXmlChunk != null, s"can't find a 'scan' tag in the scanList XML chunk:\n$scanListAsStr")

      val scanXmlChunkOffset = (scanXmlChunk - scanListAsCStr).toInt
      //println(scanXmlChunkOffset)

      val scanListCvParams = this._parseCvParams(scanListAsCStr, scanXmlChunkOffset)
      val scanListUserParams = this._parseUserParams(scanListAsCStr, scanXmlChunkOffset)
      //cvParams.foreach(c => println(c.toXml()))

      val scanWindowListXmlChunk = strstr(scanXmlChunk, scanWindowListCStr)
      assert(scanWindowListXmlChunk != null, s"can't find a 'scanWindowList' tag in the scanList XML chunk:\n$scanListAsStr")

      val scanWindowsCount = _parseAttributeContent(scanWindowListXmlChunk, 28, countCStr, countStrLen, 4)
      assert(scanWindowsCount == "1", "parsing of multiple scan windows is not yet implemented (please open an issue on github)")

      val scanWindowXmlChunk = strstr(scanWindowListXmlChunk, scanWindowCStr)
      assert(scanXmlChunk != null, s"can't find a 'scanWindow' tag in the scanList XML chunk:\n$scanListAsStr")

      val scanWindowXmlChunkOffset = (scanWindowXmlChunk - scanListAsCStr).toInt

      val scanChunkLength = scanWindowXmlChunkOffset - scanXmlChunkOffset
      val instConfRef = _parseAttributeContent(scanXmlChunk, scanChunkLength, instrumentConfRefCStr, instrumentConfRefStrLen, 16)
      val scanCvParams = this._parseCvParams(scanXmlChunk, scanChunkLength)
      val scanUserParams = this._parseUserParams(scanXmlChunk, scanChunkLength)

      val scanWindowCvParams = this._parseCvParams(scanWindowXmlChunk, scanListAsStr.length - scanWindowXmlChunkOffset)
      val scanWindowUserParams = this._parseUserParams(scanWindowXmlChunk, scanListAsStr.length - scanWindowXmlChunkOffset)

      val scanParamTree = new ScanParamTree(instConfRef)
      scanParamTree.setCVParams(scanCvParams)
      scanParamTree.setUserParams(scanUserParams)

      val scanWindowList = new ScanWindowList(scanWindowsCount.toInt)

      val scanWindow = new ScanWindow()
      scanWindow.setCVParams(scanWindowCvParams)
      scanWindow.setUserParams(scanWindowUserParams)

      scanWindowList.setScanWindows(Seq(scanWindow))
      scanParamTree.setScanWindowList(scanWindowList)

      val scanList = new ScanList(scansCount.toInt)
      scanList.setCVParams(scanListCvParams)
      scanList.setUserParams(scanListUserParams)
      scanList.setScans(Seq(scanParamTree))

      scanList
    }

  }

  private val precursorListCStr = c"<precursorList "
  private val precursorCStr = c"<precursor "

  private val spectrumRefCStr = c"""spectrumRef="""
  private val spectrumRefStrLen = strlen(spectrumRefCStr).toInt

  private val isolationWindowStartCStr = c"<isolationWindow>"
  private val isolationWindowEndCStr = c"</isolationWindow>"

  private val selectedIonListCStr = c"<selectedIonList "
  private val selectedIonStartCStr = c"<selectedIon>"
  private val selectedIonEndCStr = c"</selectedIon>"

  private val activationStartCStr = c"<activation>"
  private val activationEndCStr = c"</activation>"

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

    Zone { implicit z =>
      val precursorListChunkAsCStr = toCString(precursorListAsStr)
      val precursorListXmlChunk = strstr(precursorListChunkAsCStr, precursorListCStr)

      if (precursorListXmlChunk != null) {
        val precursorsCount = _parseAttributeContent(precursorListXmlChunk, 25, countCStr, countStrLen, 4)
        assert(precursorsCount == "1", "parsing of multiple precursors is not yet implemented (please open an issue on github)")
      }

      val precursorXmlChunk = strstr(precursorListChunkAsCStr, precursorCStr)
      val isolationWindowXmlChunk = strstr(precursorXmlChunk, isolationWindowStartCStr)
      val precursorAttrLen = if (isolationWindowXmlChunk == null) precursorListAsStr.length
      else (isolationWindowXmlChunk - precursorXmlChunk).toInt

      val spectrumRef = _parseAttributeContent(precursorXmlChunk, precursorAttrLen, spectrumRefCStr, spectrumRefStrLen, bufferSize = precursorAttrLen)

      val prec = new Precursor(if (spectrumRef == null) "" else spectrumRef)

      // --- Parse isolation window --- //
      if (isolationWindowXmlChunk != null) {
        val isolationWindowXmlChunkOffset = strstr(precursorXmlChunk, isolationWindowEndCStr) - isolationWindowXmlChunk

        val isolationWindowCvParams = this._parseCvParams(isolationWindowXmlChunk, isolationWindowXmlChunkOffset.toInt)

        val isolationWindow = new IsolationWindowParamTree()
        isolationWindow.setCVParams(isolationWindowCvParams)

        prec.setIsolationWindow(isolationWindow)
      }

      // --- Parse selected ions --- //
      val selectedIonListXmlChunk = strstr(precursorXmlChunk, selectedIonListCStr)

      if (selectedIonListXmlChunk != null) {
        val selectedIonsCount = _parseAttributeContent(selectedIonListXmlChunk, 27, countCStr, countStrLen, 4)
        assert(selectedIonsCount == "1", "parsing of multiple selected ions is not yet implemented (please open an issue on github)")

        val selectedIonXmlChunk = strstr(selectedIonListXmlChunk, selectedIonStartCStr)
        val selectedIonXmlChunkOffset = strstr(selectedIonListXmlChunk, selectedIonEndCStr) - selectedIonXmlChunk

        val selectedIonCvParams = this._parseCvParams(selectedIonXmlChunk, selectedIonXmlChunkOffset.toInt)

        val selectedIonList = new SelectedIonList(selectedIonsCount.toInt)
        val selectedIon = new SelectedIon()
        selectedIon.setCVParams(selectedIonCvParams)
        selectedIonList.setSelectedIons(Seq(selectedIon))

        prec.setSelectedIonList(selectedIonList)
      }

      // --- Parse activation type --- //
      val activationXmlChunk = strstr(precursorXmlChunk, activationStartCStr)

      if (activationXmlChunk != null) {
        val activationXmlChunkOffset = strstr(precursorXmlChunk, activationEndCStr) - activationXmlChunk

        val activationCvParams = this._parseCvParams(activationXmlChunk, activationXmlChunkOffset.toInt)

        val activation = new Activation()
        activation.setCVParams(activationCvParams)

        prec.setActivation(activation)
      }

      Seq(prec)
    }

  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    null
  }

  private val fileContentStartCStr = c"<fileContent>"
  private val fileContentEndCStr = c"</fileContent>"

  def parseFileContent(fileContentAsStr: String): FileContent = {
    /*"""<fileContent>
      |  <cvParam cvRef="MS" accession="MS:1000579" value="" name="MS1 spectrum" />
      |  <cvParam cvRef="MS" accession="MS:1000580" value="" name="MSn spectrum" />
      |</fileContent>""".stripMargin*/

    Zone { implicit z =>
      val fileContentChunkAsCStr = toCString(fileContentAsStr)

      val fileContentStartXmlChunk = strstr(fileContentChunkAsCStr, fileContentStartCStr)
      assert(fileContentStartXmlChunk != null, s"can't find a 'fileContent' start tag in the XML chunk:\n$fileContentAsStr")

      val fileContentEndXmlChunk = strstr(fileContentChunkAsCStr, fileContentEndCStr)
      assert(fileContentEndXmlChunk != null, s"can't find a 'fileContent' end tag in the XML chunk:\n$fileContentAsStr")

      val fileContentXmlChunkOffset = (fileContentEndXmlChunk - fileContentChunkAsCStr).toInt
      _parseCvParams(fileContentChunkAsCStr, fileContentXmlChunkOffset)

      val fileContentCvParams = this._parseCvParams(fileContentChunkAsCStr, fileContentXmlChunkOffset)
      val fileContentUserParams = this._parseUserParams(fileContentChunkAsCStr, fileContentXmlChunkOffset)

      val fileContent = FileContent()
      fileContent.setCVParams(fileContentCvParams)
      fileContent.setUserParams(fileContentUserParams)

      fileContent
    }
  }

  /*
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
   */

  /*def parseParamTree(paramTreeAsStr: String): ParamTree = {
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
  */

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


}