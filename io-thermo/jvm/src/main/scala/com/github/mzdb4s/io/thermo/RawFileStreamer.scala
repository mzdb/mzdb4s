package com.github.mzdb4s.io.thermo

import com.sun.jna._
import java.io.File

import thermorawfileparser.thermorawfileparser._
import thermorawfileparser.thermorawfileparser.writer.MzMlSpectrumWriter

import com.github.mzdb4s.io.reader.param._

class RawFileStreamer private[thermo](rawFilePath: File) extends AbstractRawFileStreamer(rawFilePath) {

  private var _rawFileWrapper: RawFileWrapper = {
    val parseInput = new ParseInput(rawFilePath.getCanonicalFile.getAbsolutePath, null, null, OutputFormat.MzML)
    parseInput.setUseInMemoryWriter(true)

    RawFileParser.initRawFile(parseInput)
  }

  private val _firstScanNumber = _rawFileWrapper.getFirstScanNumber
  private val _lastScanNumber = _rawFileWrapper.getLastScanNumber

  private val _mzMlWriter: MzMlSpectrumWriter = {
    val mzMlWriter = new writer.MzMlSpectrumWriter(_rawFileWrapper)
    //println(rawFileWrapper.getFirstScanNumber, rawFileWrapper.getLastScanNumber)

    mzMlWriter.createXmlWriter()
    mzMlWriter.writeHeader(_firstScanNumber, _lastScanNumber)

    mzMlWriter
  }

  private val _metaDataAsXmlString = _mzMlWriter.getInMemoryStreamAsString

  def getMetaDataAsXmlString(): String = _metaDataAsXmlString

  protected val paramTreeParser: IParamTreeParser = ParamTreeParser

  def forEachSpectrum( onEachSpectrum: RawFileSpectrum => Boolean ): Unit = {
    require(!_isConsumed, "raw file stream already consumed")

    val wrappedSpectrum = _mzMlWriter.getSpectrumWrapper

    // https://stackoverflow.com/questions/43338842/c-sharp-out-intptr-equivalent-in-java
    // https://stackoverflow.com/questions/5244214/get-pointer-of-byte-array-in-jna
    // https://stackoverflow.com/questions/40156310/jna-free-memory-created-in-the-shared-object-dll
    // https://stackoverflow.com/questions/1318682/intptr-arithmetics
    var prevPtrPeaksCount = 10000
    var mzPtr: Pointer = new Memory(prevPtrPeaksCount * 8)
    var intensityPtr: Pointer = new Memory(prevPtrPeaksCount * 8)
    var mzPtrAddress: Long = Pointer.nativeValue(mzPtr)
    var intensityPtrAddress: Long = Pointer.nativeValue(intensityPtr)

    var msCycle = 0
    var spectrumId = 0

    var sNum = _firstScanNumber
    val lastScanNum = _lastScanNumber
    while (sNum <= lastScanNum) {
      spectrumId += 1

      _mzMlWriter.resetWriter(false)
      _mzMlWriter.writeSpectrumNoReturn(sNum, sNum, false)

      val peaksCount = wrappedSpectrum.getPeaksCount
      //println("peaksCount",peaksCount)

      val xmlStr = _mzMlWriter.getInMemoryStreamAsString
      //if (sNum == 2) println(xmlStr.replaceAll("""xmlns=".+?"""",""))

      var mzList: Array[Double] = null
      var intensityList: Array[Double] = null

      if (peaksCount > prevPtrPeaksCount) {
        println(s"Reallocating mz/int buffer to accept $prevPtrPeaksCount peaks")
        Native.free(mzPtrAddress)
        Native.free(intensityPtrAddress)

        val newBuffSize = peaksCount * 8
        mzPtr = new Memory(newBuffSize)
        intensityPtr = new Memory(newBuffSize)

        mzPtrAddress = Pointer.nativeValue(mzPtr)
        intensityPtrAddress = Pointer.nativeValue(intensityPtr)

        prevPtrPeaksCount = peaksCount
      }

      if (peaksCount > 0) {
        wrappedSpectrum.copyDataToPointers(mzPtrAddress, intensityPtrAddress)
        //println( mzPtr.getDouble( (peaksCount - 1) * 8) )
        //println(wrappedSpectrum.getMzValue(peaksCount - 1))

        val mzList = new Array[Double](peaksCount)
        val intensityList = new Array[Double](peaksCount)

        mzPtr.read(0, mzList, 0, peaksCount)
        intensityPtr.read(0, intensityList, 0, peaksCount)

        /*val buffSize = peaksCount * 8
        var peakIdx = 0
        var ptrIdx = 0
        while (ptrIdx < buffSize) {

          mzList(peakIdx) = mzPtr.getDouble(ptrIdx)
          intensityList(peakIdx) = intensityPtr.getDouble(ptrIdx)

          peakIdx += 1
          ptrIdx += 8
        }*/

      } else {
        mzList = Array()
        intensityList = Array()
      }

      // FIXME: retrieve the initial ID from C# (SpectrumWrapper class)
      val (xmlMetaData, spectrumTitle) = parseSpectrumXmlMetaData(spectrumId, xmlStr)
      val msLevel = getMsLevel(xmlMetaData)

      if (msLevel == 1 ) msCycle += 1

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
      if (!continue) sNum = _lastScanNumber

      sNum += 1
    }

    Native.free(mzPtrAddress)
    Native.free(intensityPtrAddress)

    _rawFileWrapper.dispose()
    _rawFileWrapper = null

    _isConsumed = true
  }

}