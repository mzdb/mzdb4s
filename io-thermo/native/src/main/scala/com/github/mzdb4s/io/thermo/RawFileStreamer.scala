package com.github.mzdb4s.io.thermo

import java.io.File

import scala.scalanative.libc.stdlib
import scala.scalanative.libc.string.strlen
import scala.scalanative.libc.string.strstr
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import com.github.mzdb4s.db.model.SpectrumXmlMetaData
import com.github.mzdb4s.io.reader.param._
import com.github.sqlite4s.c.util.CUtils.bytes2ByteArray
import com.github.sqlite4s.c.util.CUtils.doublePtr2DoubleArray
import com.github.sqlite4s.c.util.CUtils.strcpy

import bindings.ThermoRawFileParser._

class RawFileStreamer private[thermo](rawFilePath: File) extends AbstractRawFileStreamer(rawFilePath) {

  private var _rawFileWrapper: Ptr[ThermoRawFileParser_RawFileWrapper] = {

    val parseInput = Zone { implicit z =>
      val rawFilePathAsStr = rawFilePath.getCanonicalFile.getAbsolutePath
      val rawFilePathCStr = toCString(rawFilePathAsStr)

      ThermoRawFileParser_ParseInput_new_1(
        rawFilePathCStr, //c"/mnt/d/Dev/wsl/scala-native/mzdb4s/data/small/small.RAW",
        null,
        null,
        enum_ThermoRawFileParser_OutputFormat.ThermoRawFileParser_OutputFormat_MzML
      )
    }

    ThermoRawFileParser_ParseInput_set_UseInMemoryWriter(parseInput, value = true)

    ThermoRawFileParser_RawFileParser_InitRawFile(parseInput)
  }

  private val _firstScanNumber = ThermoRawFileParser_RawFileWrapper_get_FirstScanNumber(_rawFileWrapper)
  private val _lastScanNumber = ThermoRawFileParser_RawFileWrapper_get_LastScanNumber(_rawFileWrapper)

  private val _mzMlWriter: Ptr[ThermoRawFileParser_Writer_MzMlSpectrumWriter] = {
    val mzMlWriter = ThermoRawFileParser_Writer_MzMlSpectrumWriter_new(_rawFileWrapper)

    ThermoRawFileParser_Writer_MzMlSpectrumWriter_CreateXmlWriter(mzMlWriter)
    ThermoRawFileParser_Writer_MzMlSpectrumWriter_WriteHeader(
      mzMlWriter,
      _firstScanNumber,
      _lastScanNumber
    )

    mzMlWriter
  }

  private val _metaDataAsXmlString = ThermoRawFileParser_Writer_MzMlSpectrumWriter_GetInMemoryStreamAsString(_mzMlWriter)

  def getMetaDataAsXmlString(): String = fromCString(_metaDataAsXmlString)

  protected val paramTreeParser: IParamTreeParser = ParamTreeParser

  override protected def parseSpectrumXmlMetaData(spectrumId: Int, xmlStr: String): (SpectrumXmlMetaData, String) = {

    /*
    <spectrum id="controllerType=0 controllerNumber=1 scan=1" index="1" defaultArrayLength="0" xmlns="http://psi.hupo.org/ms/mzml">
      <cvParam cvRef="MS" accession="MS:1000511" value="1" name="ms level" />
      <cvParam cvRef="MS" accession="MS:1000579" value="" name="MS1 spectrum" />
      <cvParam cvRef="MS" accession="MS:1000130" value="" name="positive scan" />
      <cvParam cvRef="MS" accession="MS:1000285" value="15245068" name="total ion current" />
      <cvParam cvRef="MS" accession="MS:1000127" value="" name="centroid spectrum" />
      <cvParam cvRef="MS" accession="MS:1000504" value="810.415222167969" name="base peak m/z" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      <cvParam cvRef="MS" accession="MS:1000505" value="1471973.875" name="base peak intensity" unitAccession="MS:1000131" unitName="number of detector counts" unitCvRef="MS" />
      <cvParam cvRef="MS" accession="MS:1000528" value="204.760070800781" name="lowest observed m/z" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      <cvParam cvRef="MS" accession="MS:1000527" value="1999.78332519531" name="highest observed m/z" unitAccession="MS:1000040" unitName="m/z" unitCvRef="MS" />
      <scanList count="1">
    */

    var paramTreeCvParams: String = ""
    var paramTreeUserParams: String = ""
    var scanList: String = ""
    var precursor: String = null

    Zone { implicit z =>
      val xmlStrAsCStr = toCString(xmlStr)
      val spectrumIndexXmlChunk = strstr(xmlStrAsCStr, c"index=")
      assert(spectrumIndexXmlChunk != null, "spectrum index attribute is missing")

      val spectrumTitleOffset = (spectrumIndexXmlChunk- xmlStrAsCStr).toInt
      val spectrumTitle = NativeParamTreeParserImpl._parseAttributeContent(xmlStrAsCStr, spectrumTitleOffset, c"id=", 3, bufferSize = spectrumTitleOffset)
      val spectrumCvParamXmlChunk = strstr(xmlStrAsCStr, c"<cvParam ")
      val spectrumUserParamXmlChunk = strstr(xmlStrAsCStr, c"<userParam ")
      val scanListStartXmlChunk = strstr(xmlStrAsCStr, c"<scanList ")
      val precursorStartXmlChunk = strstr(xmlStrAsCStr, c"<precursor ")

      assert(scanListStartXmlChunk != null,"the <scanList /> XML chunk must be present")

      // --- Parse param tree --- //
      if (spectrumCvParamXmlChunk != null) {
        val spectrumUserParamXmlChunkOffset = (scanListStartXmlChunk - spectrumUserParamXmlChunk)

        val spectrumCvParamXmlChunkOffset = if (spectrumUserParamXmlChunk != null && spectrumUserParamXmlChunkOffset > 0) {
          val spectrumUserParamsAsCStr = stackalloc[CChar]( (spectrumUserParamXmlChunkOffset + 1).toULong)
          strcpy(spectrumUserParamsAsCStr, spectrumUserParamXmlChunk, length = spectrumUserParamXmlChunkOffset.toULong)
          paramTreeUserParams = s"<userParams>\n${fromCString(spectrumUserParamsAsCStr)}\n</userParams>\n"

          (spectrumUserParamXmlChunk - spectrumCvParamXmlChunk)
        } else {
          (scanListStartXmlChunk - spectrumCvParamXmlChunk)
        }

        assert(spectrumCvParamXmlChunkOffset > 0, "unexpected position of cvParams")

        val spectrumCvParamsAsCStr = stackalloc[CChar]( (spectrumCvParamXmlChunkOffset + 1).toULong)
        strcpy(spectrumCvParamsAsCStr, spectrumCvParamXmlChunk, length = spectrumCvParamXmlChunkOffset.toULong)
        paramTreeCvParams = s"  <cvParams>\n  ${fromCString(spectrumCvParamsAsCStr)}</cvParams>\n"
      }

      // --- Parse scan list --- //
      if (scanListStartXmlChunk != null) {
        val scanListEndXmlChunk = strstr(xmlStrAsCStr, c"</scanList>") + strlen(c"</scanList>")
        val scanListXmlChunkOffset = (scanListEndXmlChunk - scanListStartXmlChunk).toULong
        val scanListXmlChunkAsCStr = stackalloc[CChar](scanListXmlChunkOffset + 1.toULong)
        strcpy(scanListXmlChunkAsCStr, scanListStartXmlChunk, length = scanListXmlChunkOffset)

        scanList = fromCString(untabstr(scanListXmlChunkAsCStr,scanListXmlChunkOffset,2))
        //println(scanList)
      }

      // --- Parse precursor --- //
      // Note: we skip the <precursorList /> tag to be consistent with the current mzDB specs (0.7)
      if (precursorStartXmlChunk != null) {
        val precursorEndXmlChunk = strstr(xmlStrAsCStr, c"</precursor>") + strlen(c"</precursor>")
        val precursorXmlChunkOffset = (precursorEndXmlChunk - precursorStartXmlChunk).toULong
        val precursorXmlChunkAsCstr = stackalloc[CChar](precursorXmlChunkOffset + 1.toULong)
        strcpy(precursorXmlChunkAsCstr, precursorStartXmlChunk, length = precursorXmlChunkOffset)
        precursor = fromCString(untabstr(precursorXmlChunkAsCstr, precursorXmlChunkOffset, 4))
      }

      val xmlMetaData = SpectrumXmlMetaData(
        spectrumId = spectrumId,
        paramTree = s"<params>\n$paramTreeCvParams$paramTreeUserParams</params>",
        scanList = scanList,
        precursorList = Option(precursor),
        productList = None
      )
      //println(spectrumTitle)
      //println(xmlMetaData.toString)

      //if (precursor != null) 1 / 0

      (xmlMetaData, spectrumTitle)
    }
  }

  /* Remove spaces/chars after each new line */
  private def untabstr(source: CString, sourceLen: CSize, nchars: Int)(implicit z: Zone): CString = {

    val dest = alloc[CChar](sourceLen) + 1

    val sourceLenAsInt = sourceLen.toInt
    var j = 0
    var i = 0
    while (i < sourceLenAsInt) {
      val c = !(source + i)
      dest(j) = !(source + i)

      if (c == '\n') {
        i += 1 + nchars
      } else {
        i += 1
      }

      j += 1
    }

    dest(j) = com.github.sqlite4s.c.util.CUtils.NULL_CHAR

    dest
  }

  /*override protected def getMsLevel(xmlMetaData: SpectrumXmlMetaData): Int = {
    // TODO: create a trait for NativeParamTreeParser/ParamTreeParser and remove duplicated code
    val paramTree = com.github.mzdb4s.io.reader.param.NativeParamTreeParser.parseParamTree(xmlMetaData.paramTree)
    val cvParams = paramTree.getCVParams()

    val defaultMsLevel = if (xmlMetaData.precursorList.isEmpty) 1 else 2
    cvParams.find(_.accession == MS_LEVEL_ACCESSION).map(_.value.toInt).getOrElse(defaultMsLevel)
  }*/

  def forEachSpectrum( onEachSpectrum: RawFileSpectrum => Boolean ): Unit = {
    require(!_isConsumed, "raw file stream already consumed")

    val wrappedSpectrum = ThermoRawFileParser_Writer_MzMlSpectrumWriter_getSpectrumWrapper(_mzMlWriter)

    var prevXmlChunkLen = 10000
    var xmlChunkPtr = stdlib.malloc(prevXmlChunkLen.toULong)
    var xmlChunkAddress: Long = xmlChunkPtr.toLong

    var prevPtrPeaksCount = 10000
    var mzPtr = stdlib.malloc(prevPtrPeaksCount.toULong * sizeof[Double])
    var intensityPtr = stdlib.malloc(prevPtrPeaksCount.toULong * sizeof[Double])
    var mzPtrAddress: Long = mzPtr.toLong
    var intensityPtrAddress: Long = intensityPtr.toLong

    var msCycle = 0
    var spectrumId = 0

    var sNum = _firstScanNumber
    val lastScanNum = _lastScanNumber
    while (sNum <= lastScanNum) {
      spectrumId += 1

      ThermoRawFileParser_Writer_MzMlSpectrumWriter_ResetWriter(_mzMlWriter, callGC = false)
      ThermoRawFileParser_Writer_MzMlSpectrumWriter_WriteSpectrumNoReturn(_mzMlWriter, sNum, sNum, serializeBinaryData = false)

      //val xmlStr = fromCString(ThermoRawFileParser_Writer_MzMlSpectrumWriter_GetInMemoryStreamAsString(_mzMlWriter))
      //println(xmlStr)

      val xmlChunkLen = ThermoRawFileParser_Writer_MzMlSpectrumWriter_FlushWriterThenGetXmlStreamLength(_mzMlWriter)

      if (xmlChunkLen > prevXmlChunkLen) {
        logger.debug(s"Reallocating XML chunk buffer to accept $xmlChunkLen chars")

        xmlChunkPtr = stdlib.realloc(xmlChunkPtr, xmlChunkLen.toULong)
        assert(xmlChunkPtr != null, "xmlChunkPtr is null, realloc failed")

        xmlChunkAddress = xmlChunkPtr.toLong
        prevXmlChunkLen = xmlChunkLen
      }

      val xmlStrAsBytes = if (xmlChunkLen == 0) Array.empty[Byte]
      else {
        ThermoRawFileParser_Writer_MzMlSpectrumWriter_CopyXmlStreamToPointers(_mzMlWriter, xmlChunkAddress)
        bytes2ByteArray(xmlChunkPtr, xmlChunkLen.toULong)
      }

      val xmlStr: String = new String(xmlStrAsBytes, java.nio.charset.StandardCharsets.UTF_8)
      //println(xmlStr)

      // FIXME: retrieve the initial ID from C# (SpectrumWrapper class)
      val (xmlMetaData, spectrumTitle) = parseSpectrumXmlMetaData(spectrumId, xmlStr)

      val peaksCount = ThermoRawFileParser_Writer_SpectrumWrapper_getPeaksCount(wrappedSpectrum)
      //println("peaksCount",peaksCount)

      var mzList: Array[Double] = null
      var intensityList: Array[Double] = null

      if (peaksCount > prevPtrPeaksCount) {
        logger.debug(s"Reallocating mz/int buffer to accept $peaksCount peaks")

        val newBuffSize = (peaksCount * 8).toULong
        mzPtr = stdlib.realloc(mzPtr, newBuffSize * sizeof[Double])
        intensityPtr = stdlib.realloc(intensityPtr, newBuffSize * sizeof[Double])
        assert(mzPtr != null, "mzPtr is null, realloc failed")
        assert(intensityPtr != null, "intensityPtr is null, realloc failed")

        mzPtrAddress = mzPtr.toLong
        intensityPtrAddress = intensityPtr.toLong

        prevPtrPeaksCount = peaksCount
      }

      if (peaksCount > 0) {
        ThermoRawFileParser_Writer_SpectrumWrapper_CopyDataToPointers(wrappedSpectrum, mzPtrAddress, intensityPtrAddress)

        mzList = doublePtr2DoubleArray(mzPtr.asInstanceOf[Ptr[Double]], peaksCount)
        intensityList = doublePtr2DoubleArray(intensityPtr.asInstanceOf[Ptr[Double]], peaksCount)
      } else {
        mzList = Array()
        intensityList = Array()
      }

      val msLevel = getMsLevel(xmlMetaData)

      if (msLevel == 1) msCycle += 1

      val newSpec = this.createSpectrum(
        spectrumId,
        sNum,
        msLevel,
        msCycle,
        spectrumTitle,
        xmlMetaData,
        mzList,
        intensityList
      )

      val continue = onEachSpectrum(newSpec)
      //val continue = onEachSpectrum(null)
      if (!continue) sNum = lastScanNum

      sNum += 1
    }

    stdlib.free(xmlChunkPtr)
    stdlib.free(mzPtr)
    stdlib.free(intensityPtr)

    ThermoRawFileParser_RawFileWrapper_Dispose(_rawFileWrapper)
    _rawFileWrapper = null

    _isConsumed = true
  }

}
