package com.github.mzdb4s.io.thermo

import java.io.File

import scala.scalanative.libc.stdlib
import scala.scalanative.unsafe._
import bindings.ThermoRawFileParser._

import com.github.mzdb4s.db.model.SpectrumXmlMetaData
import com.github.mzdb4s.io.reader.param._
import com.github.sqlite4s.c.util.CUtils.doublePtr2DoubleArray

class RawFileStreamer private[thermo](rawFilePath: File) extends AbstractRawFileStreamer(rawFilePath) {

  private var _rawFileWrapper: Ptr[ThermoRawFileParser_RawFileWrapper] = {

    val parseInput = Zone { implicit z =>
      val rawFilePathAsStr = rawFilePath.getCanonicalFile.getAbsolutePath
      println(rawFilePathAsStr)
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

  protected val paramTreeParser: IParamTreeParser = NativeParamTreeParser

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

    var prevPtrPeaksCount = 10000
    var mzPtr = stdlib.malloc(prevPtrPeaksCount * sizeof[Double])
    var intensityPtr = stdlib.malloc(prevPtrPeaksCount * sizeof[Double])
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

      val peaksCount = ThermoRawFileParser_Writer_SpectrumWrapper_getPeaksCount(wrappedSpectrum)
      //println("peaksCount",peaksCount)

      val xmlStr = ThermoRawFileParser_Writer_MzMlSpectrumWriter_GetInMemoryStreamAsString(_mzMlWriter)

      var mzList: Array[Double] = null
      var intensityList: Array[Double] = null

      if (peaksCount > prevPtrPeaksCount) {
        println(s"Reallocating mz/int buffer to accept $prevPtrPeaksCount peaks")

        val newBuffSize = peaksCount * 8
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

      // FIXME: retrieve the initial ID from C# (SpectrumWrapper class)
      val (xmlMetaData, spectrumTitle) = parseSpectrumXmlMetaData(spectrumId, fromCString(xmlStr))
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

    stdlib.free(mzPtr)
    stdlib.free(intensityPtr)

    ThermoRawFileParser_RawFileWrapper_Dispose(_rawFileWrapper)
    _rawFileWrapper = null

    _isConsumed = true
  }

}
