package com.github.mzdb4s.io.mzml

import java.io.{File, RandomAccessFile}

import scala.collection.Seq
import scala.collection.mutable.ArrayBuffer
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._
import scala.scalanative.libc.stdio._
import scala.scalanative.libc.stdlib._
import scala.scalanative.libc.string._
import scala.scalanative.libc.ctype.isdigit

import com.github.utils4sn._
import com.github.utils4sn.YxmlUtils.parseXmlChunk
import com.github.utils4sn.bindings.Base64Lib
import com.github.sqlite4s.c.util._

import com.github.mzdb4s.msdata._
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._
//import fr.profi.mzdb.model.xml._
import com.github.mzdb4s.util.io.FastReversedLineInputStream

object MzMLParser {
  // Define some XML tags for index parsing
  private val indexListOffsetStartTag = "<indexListOffset>"
  private val indexListOffsetStartTagLen = indexListOffsetStartTag.length
  private val offsetRegex = """\s*<offset idRef="(.+)">(\d+)</offset>\s*""".r

  // Define some XML tags for meta-data parsing of <spectrum> sections
  private val scanListEndTag = c"</scanList>"
  private val scanListEndTagLen: CInt = strlen(scanListEndTag).toInt

  private val precursorListStartTag = c"<precursorList"
  private val precursorListEndTag = c"</precursorList>"
  private val precursorListEndTagLen: CInt = strlen(precursorListEndTag).toInt

  private val paramTreeHeader = c"<params>\n  <cvParams>\n    "
  private val paramTreeHeaderLen = strlen(paramTreeHeader)
  private val paramTreeFooter = c"</cvParams>\n  <userParams>\n  </userParams>\n</params>"
  private val paramTreeFooterLen = strlen(paramTreeFooter)
// TODO: add this userParam??? <userParam cvRef="MS" accession="-1" name="in_high_res" type="boolean" value="false" />
}

class MzMLParser(file: File) extends java.io.Closeable {

  def this(fileLocation: String) {
    this(new File(fileLocation))
  }

  // TODO: eliminate the use of RAF
  private val raf = new RandomAccessFile(file, "r")

  private var _opened_file: Ptr[FILE] = null

  Zone { implicit z =>
    val fname = CUtils.toCString(file.getAbsolutePath)
    _opened_file = fopen(fname,c"r")
  }

  override def close(): Unit = {
    raf.close()
    fclose(_opened_file)
  }

  private val mzMLIndex: MzMLIndex = {
    val idxOpt = _loadIndex()
    assert(idxOpt.nonEmpty, s"unable to load index from MzML file '$file'")
    idxOpt.get
  }

  def getMetaData(): MzMLMetaData = {

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
        dataProcessingName = "ProteoWizard file conversion",
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
      // TODO: CVParams
      Software(id = 1, name = "Xcalibur", version = "2.7.0 SP1", paramTree = null),
      Software(id = 1, name = "pwiz", version = "3.0.5759", paramTree = null),
      Software(id = 1, name = "pwiz-mzdb", version = "0.9.10", paramTree = null), // TODO: remove me when mzdb-access is updated
      Software(id = 1, name = "mzdb4s", version = "0.2.1910", paramTree = null) // TODO: retrieve from file generated by SBT
    )

    val sourceFiles = Seq(SourceFile(id = 1, name = "OVEMB150205_12.raw", location = "", paramTree = null))

    MzMLMetaData(
      FileContent(), // FIXME: parse FileContent
      commonInstrumentParams,
      instrumentConfigurations,
      processingMethods,
      runs,
      samples,
      softwareList,
      sourceFiles
    )
  }

  def forEachSpectrumChunk(fn: (MzMLSpectrumChunk, Int) => Boolean): Unit = {

    var specNumber = 1
    for (specIndex <- mzMLIndex.spectraIndexes) {
      /*
        println("chunk ref: " + specIndex.ref)
        println("chunk start: " + specIndex.start)
        println("chunk len: " + specIndex.length)
      */

      /*val buffer = new Array[Byte](specIndex.length)
      raf.seek(specOffset.start)
      raf.read(buffer)*/

      fseek( _opened_file, specIndex.start, SEEK_SET )

      // TODO: reuse specBuffer between all iterations ?
      val specBuffer: Ptr[CChar] = calloc(specIndex.length.toULong, 1L.toULong)
      assert(specBuffer != null, "forEachSpectrumChunk: can't allocate memory for specBuffer")

      if (fread(specBuffer, specIndex.length.toULong, 1.toULong, _opened_file) != 1.toULong) {
        throw new Exception(s"Error while reading spectrum #$specNumber located at offset ${specIndex.start} in file '$file'")
      }

      // TODO: provide the zone out of forEachSpectrumChunk??
      Zone { implicit z =>

        val specChunk = try {
          new MzMLSpectrumChunk(specIndex.ref, specBuffer)
        } catch {
          case t: Throwable => {
            println("And error occurred during the parsing of the spectrum XML chunk: " + t.getMessage)
            null
          }
        }

        val continue = try {
          if (specChunk != null) {
            //println(s"Processing spectrum '${specChunk.id}'")
            fn(specChunk, specNumber)
          }
          true
        } finally {
          //specChunk.destroy() // free memory
          free(specBuffer) // free buffer
        }

        if (continue == false) return
      }

      specNumber += 1
    }
  }

  def forEachSpectrum(fn: (Spectrum, SpectrumXmlMetaData, DataEncoding) => Boolean): Unit = {

    // FIXME: determine the ByteOrder
    val bo = java.nio.ByteOrder.LITTLE_ENDIAN

    // FIXME: create alternative DEs for NO LOSS mode (64-bit intensities)
    val profileHighResDE = DataEncoding(id = 1, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val profileLowResDE = DataEncoding(id = 2, mode = DataMode.PROFILE, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedHighResDE = DataEncoding(id = 3, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.HIGH_RES_PEAK, compression = "none", byteOrder = bo)
    val centroidedLowResDE = DataEncoding(id = 4, mode = DataMode.CENTROID, peakEncoding = PeakEncoding.LOW_RES_PEAK, compression = "none", byteOrder = bo)

    val paramTreeCStrBuilder = CStringBuilder.create()

    //var previousMsLevel = 0
    var msCycle = 0
    this.forEachSpectrumChunk { (chunk: MzMLSpectrumChunk, spectrumNumber: Int) =>
      val spectrumTitle = chunk.id

      //println(fromCString(chunk.spectrumHeaderChunk))

      //Zone { implicit z =>
      //  val xml = fromCString(chunk.xmlString.get)
      //  println("xml: " +chunk.xmlString.get )
      //}

      assert(chunk.mzVector != null, s"unable to retrieve an mzVector from spectrum chunk with id = '$spectrumTitle'")

      val peaksCount = chunk.peaksCount.toInt
      //println(s"got $peaksCount peaks 64bits = ${chunk.mzArrayMetaData.is64Bits}")

      //val chunkMetaData = fromCString(chunk.spectrumHeaderChunk)
      //println(chunkMetaData)
      // TODO: parse first <spectrum index="0" id="controllerType=0 controllerNumber=1 scan=1" defaultArrayLength="13921">

      // Spectrum header
      var msLevel: Int = 1
      var isProfileSpectrum = false
      var basePeakMz = -1.0
      var basePeakIntensity = -1f
      var tic = -1f // TODO: do we need to compute it ourself?
      var scanTime = -1f

      var isoWinTarget = -1f
      var isoWinLowerOffset = -1f
      var isoWinUpperOffset = -1f

      // Spectrum meta-data
      //var paramTreeSizePtr = stackalloc[CSize]
      var paramTreeStr: String = null
      var scanListStr: String = null
      var precursorListStrOpt: Option[String] = None
      var productListStrOpt: Option[String] = None

      val callbackByTagName: Map[String, XmlNode => CBool] = Map(
        // Parse all cvParam nodes of the current spectrum chunk
        "cvParam" -> { xmlNode: XmlNode =>
          val attrValueByKey = xmlNode.attributes.toMap
          val ac = attrValueByKey("accession")
          def value(): String = attrValueByKey("value")

          // FIXME: parse the precursor information (selectedIon and activation type)
          ac match {
            // common spectra meta-data
            case "MS:1000511" => msLevel = value().toInt
            case "MS:1000128" => isProfileSpectrum = true
            case "MS:1000504" => basePeakMz = value().toDouble
            case "MS:1000505" => basePeakIntensity = value().toFloat
            case "MS:1000285" => tic = value().toFloat
            case "MS:1000016" => {
              val rt = value().toFloat
              val isUnitMinutes = attrValueByKey("unitName") == "minute"
              scanTime = if (isUnitMinutes) rt * 60 else rt
            }
            // isolation windows meta-data (only for MSn data)
            case "MS:1000827" => isoWinTarget = value().toFloat
            case "MS:1000828" => isoWinLowerOffset = value().toFloat
            case "MS:1000829" => isoWinUpperOffset = value().toFloat
            case _ => {}
          }

          true
        },
        "scanList" -> { xmlNode =>

          paramTreeCStrBuilder.clear()
          paramTreeCStrBuilder.addString(MzMLParser.paramTreeHeader, MzMLParser.paramTreeHeaderLen)

          // --- <spectrum> CV params parsing --- //
          val scanListOffset = xmlNode.offset - 10 // 10 is length of "<scanList" + 1
          val cvParamsOffset = CUtils.substr_idx(chunk.spectrumHeaderChunk, chunk.cvParamCStr)
          //val spectrumCvParams = CUtils.fromCString(chunk.spectrumHeaderChunk + cvParamsOffset, scanListOffset - cvParamsOffset)
          //println( "spectrumCvParams: " + spectrumCvParams)

          paramTreeCStrBuilder.addString(chunk.spectrumHeaderChunk + cvParamsOffset, (scanListOffset - cvParamsOffset).toULong)
          paramTreeCStrBuilder.addString(MzMLParser.paramTreeFooter, MzMLParser.paramTreeFooterLen)

          paramTreeStr = CUtils.fromCString(paramTreeCStrBuilder.underlyingString())

          // Make the string prettier
          // TODO: static strings
          paramTreeStr = paramTreeStr.replace("          </cvParams>", "  </cvParams>")
          paramTreeStr = paramTreeStr.replaceAllLiterally("          ", "    ")
          //println( "spectrumCvParams: " + paramTreeStr)

          // --- <scanList> parsing --- //
          val scanListChunk: Ptr[CChar] = chunk.spectrumHeaderChunk + scanListOffset
          val scanListEndChunk = strstr(scanListChunk, MzMLParser.scanListEndTag)
          assert(
            scanListEndChunk != null,
            s"can't find </scanList> XML tag in spectrum chunk with id = '$spectrumTitle'"
          )

          val scanListStrEndPos = scanListEndChunk - scanListChunk + MzMLParser.scanListEndTagLen
          scanListStr = CUtils.fromCString(scanListChunk, scanListStrEndPos.toULong)

          // Make the string prettier
          // TODO: static strings
          scanListStr = scanListStr.replace("          </scanList>", "</scanList>")
          scanListStr = scanListStr.replaceAllLiterally("            ", "  ")
          //println( "scanList: " + scanListStr)

          // --- <precursorList> parsing --- //
          val precursorListChunk = strstr(scanListEndChunk, MzMLParser.precursorListStartTag)
          if (precursorListChunk != null) {
            val precursorListEndChunk = strstr(precursorListChunk, MzMLParser.precursorListEndTag)
            assert(
              precursorListEndChunk != null,
              s"can't find </precursorList> XML tag in spectrum chunk with id = '$spectrumTitle'"
            )

            val precursorListStrEndPos = precursorListEndChunk - precursorListChunk + MzMLParser.precursorListEndTagLen
            var precursorListStr = CUtils.fromCString(precursorListChunk, precursorListStrEndPos.toULong)

            // Make the string prettier
            // TODO: static strings???
            precursorListStr = precursorListStr.replace("          </precursorList>", "</precursorList>")
            precursorListStr = precursorListStr.replaceAllLiterally("            ", "  ")

            precursorListStrOpt = Some(precursorListStr)
            //println( "scanList: " + scanListStr)
            //println("precursorListStr: " + precursorListStr)
          }

          true
        }
        /*"scanWindowList" -> { xmlNode =>
          false
        } // stop to parse cvParams when we meet scanWindowList*/
      )

      Zone { implicit z =>
        // TODO: to make it faster we could parse all CvParams using strstr instead of using Yxml (use _scanCvParamAccession() as a starting point)
        parseXmlChunk(chunk.spectrumHeaderChunk, callbackByTagName)
      }

      // FIXME: This might be more complicated for DIA
      if (msLevel == 1) {
        msCycle += 1
      }
      //previousMsLevel = msLevel


      val isoWin = if (isoWinTarget == -1f || isoWinLowerOffset == -1f || isoWinUpperOffset == -1f) None
      else {
        Some(IsolationWindow(isoWinTarget - isoWinLowerOffset, isoWinTarget + isoWinUpperOffset, isoWinTarget))
      }

      val sh = SpectrumHeader(
        id = spectrumNumber,
        initialId = spectrumNumber,
        title = spectrumTitle,
        cycle = msCycle,
        time = scanTime,
        msLevel = msLevel,
        activationType = Some(ActivationType.CID), // FIXME: parse activation tag from precursorList
        peaksCount = peaksCount,
        isHighResolution = true,
        tic = tic,
        basePeakMz = basePeakMz,
        basePeakIntensity = basePeakIntensity,
        precursorMz = None,
        precursorCharge = None,
        bbFirstSpectrumId = 1,
        isolationWindow = isoWin
      )

      val smdAsXml = SpectrumXmlMetaData(
        // FIXME: retrieve activation type
        spectrumId = spectrumNumber,
        paramTree = paramTreeStr,
        scanList = scanListStr,
        precursorList = precursorListStrOpt,
        productList = productListStrOpt
      )

      var dataEncoding: DataEncoding = null

      // TODO: use the lower level data representation to avoid data conversion
      val mzList = if (chunk.mzArrayMetaData.is64Bits) {
        dataEncoding = if (isProfileSpectrum) profileHighResDE else centroidedHighResDE

        if (peaksCount == 0) Array.empty[Double]
        else CUtils.doublePtr2DoubleArray(chunk.mzVector.ptr.asInstanceOf[Ptr[CDouble]], peaksCount)
      } else {
        dataEncoding = if (isProfileSpectrum) profileLowResDE else centroidedLowResDE

        if (peaksCount == 0) Array.empty[Double]
        else CUtils.floatPtr2FloatArray(chunk.mzVector.ptr.asInstanceOf[Ptr[CFloat]], peaksCount).map(_.toDouble)
      }

      val intensityList = if (peaksCount == 0) Array.empty[Float]
      else {
        if (chunk.intensityArrayMetaData.is64Bits) {
          CUtils.doublePtr2DoubleArray(chunk.intensityVector.ptr.asInstanceOf[Ptr[CDouble]], peaksCount).map(_.toFloat)
        } else {
          CUtils.floatPtr2FloatArray(chunk.intensityVector.ptr.asInstanceOf[Ptr[CFloat]], peaksCount)
        }
      }

      val sd = SpectrumData(
        mzList = mzList,
        // FIXME: check intensity data precision
        intensityList = intensityList,
        leftHwhmList = null,
        rightHwhmList = null
      )

      val s = Spectrum(sh, sd)

      fn(s, smdAsXml, dataEncoding)
    }

    // Free the CString builder
    paramTreeCStrBuilder.destroy()
  }

  private def _loadIndex(): Option[MzMLIndex] = {

    // TODO: use RAF directly (only read a small chunk of XML)
    val reader = FastReversedLineInputStream.createBufferedReader(file, 256)
    val fileSize = file.length
    val fileSizeCharsLen = fileSize.toString.length
    println("file size: "+ fileSize)
    println("fileSizeCharsLen: "+ fileSizeCharsLen)

    val tmpMzMLIndex = try {

      var indexListOffset = -1L

      var i = 0
      while (i < 10) {
        val line = reader.readLine()
        val lineLen = line.length
        var idx = line.indexOf(MzMLParser.indexListOffsetStartTag)

        // Check if the line contains the <indexListOffset> tag
        if (idx != -1) {
          idx += MzMLParser.indexListOffsetStartTagLen

          val offsetCharArray = new Array[Char](fileSizeCharsLen)
          var cPos = 0

          var char: Char = 0
          while (idx < lineLen) {
            char = line.charAt(idx)
            if (char == '<') {
              idx = lineLen
            } else {
              offsetCharArray(cPos) = char
              idx += 1
              cPos += 1
            }
          }

          // FIXME: _charArrayToLong is broken
          indexListOffset = new String(offsetCharArray).toLong //_charArrayToLong(offsetCharArray, 0, cPos)
          println("index offset char array: " + new String(offsetCharArray))
        }

        i += 1
      }

      // Return None if we didn't found the indexListOffset value
      if (indexListOffset == -1) return None
      println(s"indexListOffset: $indexListOffset")

      val bufferLen = (fileSize - indexListOffset).toInt - 10
      println(s"bufferLen: $bufferLen")

      val buffer = new Array[Byte](bufferLen)
      raf.seek(indexListOffset)
      raf.read(buffer)
      raf.close()

      val lines = new String(buffer).split("\n")
      val linesCount = lines.length
      val spectrumIndexLineIdx = lines.indexWhere(_ contains """<index name="spectrum">""") //<index name="spectrum">
      println(s"spectrumIndexLineIdx: $spectrumIndexLineIdx")
      println(s"linesCount: $linesCount")

      val offsets = new ArrayBuffer[MzMLSpectrumOffset](linesCount)
      var lineIdx = spectrumIndexLineIdx + 1

      while (lineIdx < linesCount) {
        val line = lines(lineIdx)
        if (line contains "</index>") {
          lineIdx = linesCount
        }
        else {

          offsets += _parseMzMLSpectrumOffset(line)

          lineIdx += 1
        }
      }

      println("Read #offset: "+offsets.length)

      // FIXME: compute the last spectrum index => use indexOf </spectrum> on the last buffer
      //val spectraIndexes = new Array[MzMLSpectrumIndex](offsets.length)
      val spectraIndexes = new Array[MzMLSpectrumIndex](offsets.length - 1)

      var oIdx = 0
      offsets.sliding(2).foreach { offsetPair =>
        val first = offsetPair.head
        val second = offsetPair.last

        spectraIndexes(oIdx) = MzMLSpectrumIndex(first.ref, first.offset, (second.offset - first.offset).toInt )

        oIdx += 1
      }

      // Search for the end of the last spectrum
      /*println("Search for the end of the last spectrum")
      val lastOffset = offsets.last
      raf.seek(lastOffset.offset)
      println("after seek")

      var endOfLineSearch = false
      var endOfLastSpectrumIndex = -1L
      while (endOfLineSearch == false) {
        val line = raf.readLine()
        println("line: " + line)

        if (line == null) endOfLineSearch = true
        else if (line.contains("</spectrum>")) {
          endOfLastSpectrumIndex = raf.getFilePointer
          //println("found last index: " + endOfLastSpectrumIndex)
          endOfLineSearch = true
        }
      }
      if (endOfLastSpectrumIndex == -1) return None

      // FIXME: load the chromatogram index first
      spectraIndexes(spectraIndexes.length - 1) = MzMLSpectrumIndex(
        lastOffset.ref,
        lastOffset.offset,
        (endOfLastSpectrumIndex - lastOffset.offset).toInt
      )*/

      println("Read #spectraIndexes: "+spectraIndexes.length)

      MzMLIndex(spectraIndexes)

    } finally {
      reader.close()
    }

    Some(tmpMzMLIndex)
  }


  private def _parseMzMLSpectrumOffset(line: String): MzMLSpectrumOffset = {

    // FIXME: there is a memory leak in SN when use this kind of parsing
    /*val spectrumOffset1 = line match {
      case MzMLParser.offsetRegex(id, offset) => MzMLSpectrumOffset(id, offset.toLong)
      case _ => throw new Exception("can't parse offset from line: " + line.take(100))
    }*/

    assert(line.contains("</offset>"),"can't parse offset from line: " + line.take(100))

    val idBuffer = new ArrayBuffer[Char](line.length)
    val offsetBuffer = new ArrayBuffer[Char](line.length)

    var inId = false
    var inOffset = false
    line.toCharArray.foreach { char =>
      if (char == '"') inId = !inId
      else if (offsetBuffer.isEmpty && char == '>') inOffset = true
      else if (inOffset && char == '<') inOffset = false
      else {
        if (inId) idBuffer += char
        else if (inOffset) offsetBuffer += char
      }
    }

    val(id,offset) = (new String(idBuffer.toArray),new String(offsetBuffer.toArray))
    /*println("line: "+ line)
    println(offsetBuffer.length)
    println(offset.length)
    println(s"id='$id', offset='$offset'")*/

    val spectrumOffset2 = MzMLSpectrumOffset(id, offset.toLong)
    //assert(spectrumOffset1 == spectrumOffset2,s"$spectrumOffset1 != $spectrumOffset2")

    spectrumOffset2
  }

  @throws[NumberFormatException]
  private def _charArrayToLong(data: Array[Char], start: Int, end: Int): Long = {
    var result = 0L

    var i = start
    while ( i < end) {
      // compute the difference between 'data(i)' ASCII CODE and ZERO_ASCII_CODE to obtain the integer value
      val digit = data(i).toInt - CUtils.ZERO_ASCII_CODE
      //if ((digit < 0) || (digit > 9)) throw new NumberFormatException
      result *= 10 // multiple previous number by 10
      result += digit // add new digit

      i += 1
    }

    result
  }

}

case class MzMLIndex(spectraIndexes: Array[MzMLSpectrumIndex])
case class MzMLSpectrumOffset(ref: String, offset: Long)
case class MzMLSpectrumIndex(ref: String, start: Long, length: Int)

object CvParamAccession {
  val intensityArray = "MS:1000515" // unitCvRef="MS" unitAccession="MS:1000131" unitName="number of detector counts"
  val mzArray = "MS:1000514" // unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"
  val noCompression = "MS:1000576"
  val precision32Bits = "MS:1000521"
  val precision64Bits = "MS:1000523"
}

case class MzMLBinaryChunkMetaData(
  encodedLength: CSize,
  cvParams: Seq[(String,(String,String))]
) {
  import CvParamAccession._

  private val _cvParamAcSet = cvParams.map(_._1).toSet

  lazy val is32Bits = _cvParamAcSet.contains(precision32Bits)
  lazy val is64Bits = _cvParamAcSet.contains(precision64Bits)
  lazy val isIntensityArray = _cvParamAcSet.contains(intensityArray)
  lazy val isMzArray  = _cvParamAcSet.contains(mzArray)
  lazy val isUncompressed = _cvParamAcSet.contains(noCompression)
}

object MzMLSpectrumChunk {
  private val spectrumEndTag = c"</spectrum>"
  private val spectrumEndTagLen: CInt = strlen(spectrumEndTag).toInt

  private val binaryDataArrayListStartTag = c"<binaryDataArrayList"
  private val encodedLengthRegex = """(?s).+encodedLength\s*=\s*"(\d+)"\s*>.+""".r
  private val cvParamRegex = """(?s).+?<cvParam\s+cvRef=".+?"\s+accession="(\w+?:\w+?)"\s+name="(.+?)" value="(.*?)".*?""".r

  private val binaryStartTag = c"<binary>"
  private val binaryStartTagLen: CInt = strlen(binaryStartTag).toInt
}

class MzMLSpectrumChunk(val id: String, var xml_c_str: CString)(implicit z: Zone) {

  import MzMLSpectrumChunk._

  def xmlString: Option[String] = if (xml_c_str == null) None else Some(fromCString(xml_c_str))

  /** Allocated memory managed by the provided zone **/
  var spectrumHeaderChunk: CString = _

  /** Allocated memory managed by the provided zone **/
  var mzArrayMetaData: MzMLBinaryChunkMetaData = _

  /** Allocated memory managed by the provided zone **/
  var intensityArrayMetaData: MzMLBinaryChunkMetaData = _

  var mzVector: PtrBox = _

  var intensityVector: PtrBox = _

  var peaksCount = -1L

  _parse_chunk()

  /** This method will split the spectrum element in 2 parts: a <spectrum> element including all meta-data
    * <binaryDataArrayList> element containing the MS data.
    */
  private def _parse_chunk(): Unit = {

    /** Substring <binaryDataArrayList> element **/
    val binaryDataArrayListChunk = strstr(xml_c_str, binaryDataArrayListStartTag)
    assert(
      binaryDataArrayListChunk != null,
      s"can't find <binaryDataArrayList> XML tag in spectrum chunk with id = '$id'"
    )

    /** Substring spectrum header and append '</spectrum>' tag to create a valid chunk **/
    val offset: CPtrDiff = binaryDataArrayListChunk - xml_c_str

    val spectrumHeaderChunkLen = offset + spectrumEndTagLen
    spectrumHeaderChunk = alloc[CChar]( (spectrumHeaderChunkLen + 1).toULong )
    assert(spectrumHeaderChunk != null, "_parse_chunk: can't allocate memory")

    // Copy wanted XML portions (cvParams, meta-data, and spectrumEndTag)
    memcpy( spectrumHeaderChunk, xml_c_str, offset.toULong )
    memcpy( spectrumHeaderChunk + offset, spectrumEndTag, spectrumEndTagLen.toULong )
    spectrumHeaderChunk(spectrumHeaderChunkLen) = CUtils.NULL_CHAR // null terminator!

    val mz_binary_sub_str = _substr_binary_xml_chunk(binaryDataArrayListChunk)
    mzArrayMetaData = _parse_binary_chunk_meta_data(binaryDataArrayListChunk, mz_binary_sub_str)
    //mzArrayMetaData = _parse_binary_chunk_meta_data_slow(binaryDataArrayListChunk)

    assert(
      mzArrayMetaData.isMzArray,
      s"unexpected binary chunk data type for spectrum ith id='$id' (m/z array expected)"
    )

    val mzPtrBox = _parse_vector_from_binary_chunk(
      mz_binary_sub_str,
      mzArrayMetaData
    )
    this.mzVector = mzPtrBox

    val( firstPeakMz, mzPeaksCount ) = if (mzArrayMetaData.is64Bits) {
      val mz_ptr = mzPtrBox.ptr.asInstanceOf[Ptr[CDouble]]

      (mz_ptr(0), mzPtrBox.length.toLong / 8L)

    } else {
      val mz_ptr = mzPtrBox.ptr.asInstanceOf[Ptr[CFloat]]

      (mz_ptr(0), mzPtrBox.length.toLong / 4L)
    }

    //println(s"First m/z = $firstPeakMz")

    val secondBinaryDataArrayChunk = mz_binary_sub_str + mzArrayMetaData.encodedLength // skip mz B64 data
    val intensity_binary_sub_str = _substr_binary_xml_chunk(secondBinaryDataArrayChunk)

    intensityArrayMetaData = _parse_binary_chunk_meta_data(secondBinaryDataArrayChunk, intensity_binary_sub_str)
    assert(
      intensityArrayMetaData.isIntensityArray,
      s"unexpected binary chunk data type for spectrum ith id='$id' (intensity array expected)"
    )

    val intensityPtrBox = _parse_vector_from_binary_chunk(
      intensity_binary_sub_str,
      intensityArrayMetaData
    )
    this.intensityVector = intensityPtrBox

    val( firstPeakIntensity, intPeaksCount ) = if (intensityArrayMetaData.is64Bits) {
      val intensity_ptr = intensityPtrBox.ptr.asInstanceOf[Ptr[CDouble]]

      (intensity_ptr(0), intensityPtrBox.length.toLong / 8L)

    } else {
      val intensity_ptr = intensityPtrBox.ptr.asInstanceOf[Ptr[CFloat]]

      (intensity_ptr(0), intensityPtrBox.length.toLong / 4L)
    }
    //println("PeaksCount = " + intPeaksCount)

    assert(
      mzPeaksCount == intPeaksCount,
      "inconsistent number of peaks between m/z and intensity arrays, something maybe wrong with data compression or encoding"
    )

    peaksCount = mzPeaksCount

    //println(s"First peak: $firstPeakMz ; $firstPeakIntensity")
    //println(fromCString(secondBinaryDataArrayChunk))
  }

  /** Scan the binaryDataArrayXmlChunk to reach the <binary> start tag */
  private def _substr_binary_xml_chunk(binaryDataArrayXmlChunk: CString): CString = {

    // Substring first <binary> element
    var binaryXmlChunkCursor: CString = strstr(binaryDataArrayXmlChunk, binaryStartTag)
    assert(binaryXmlChunkCursor != null, s"can't find <binary> XML tag in spectrum chunk with id = '$id'")
    binaryXmlChunkCursor += binaryStartTagLen

    binaryXmlChunkCursor
  }

  // TODO: move to MzMLSpectrumChunk object
  lazy val encodedLengthCStr = c"""encodedLength="""
  lazy val encodedLengthStr = "encodedLength="
  lazy val encodedLengthStrLen = encodedLengthStr.length

  lazy val cvParamCStr = c"<cvParam"
  /*lazy val encodedLengthCharSum: Int = {
    val charsAsInts = encodedLengthStr.toCharArray.map { c =>
      c.toInt
    }

    charsAsInts.sum
  }*/

  private def _parse_binary_chunk_meta_data(binaryDataArrayXmlChunk: CString, binaryXmlChunk: CString): MzMLBinaryChunkMetaData = {

    // Compute offset between the two string pointers
    val metaDataLength = binaryXmlChunk - binaryDataArrayXmlChunk
    assert(metaDataLength < 1024, s"meta-data length ($metaDataLength) is too large, maximum length is 1024")

    //println("encodedLengthCharSum: "+ encodedLengthCharSum)
    val binaryDataArrayMetaDataAsCString: CString = stackalloc[CChar](1024)
    // TODO: avoid copy if possible
    CUtils.strcpy(binaryDataArrayMetaDataAsCString, binaryDataArrayXmlChunk, metaDataLength.toULong)

    /*if (id == "controllerType=0 controllerNumber=1 scan=21161") {
      println("length of binaryDataArrayMetaDataAsCString: "+fromCString(binaryDataArrayMetaDataAsCString).length)
      println("binaryDataArrayMetaDataAsCString:\n"+fromCString(binaryDataArrayMetaDataAsCString))
    }*/

    // Search for 'encodedLength="'
    val encodedLengthChunk = strstr(binaryDataArrayMetaDataAsCString, encodedLengthCStr)
    assert(
      encodedLengthChunk != null,
      s"can't find 'encodedLength' in spectrum chunk with id = '$id'"
    )

    //val encodedLengthAsStr: CString = stackalloc[CChar](encodedLengthStrLen + 1)
    //encodedLengthAsStr( encodedLengthStrLen ) = CUtils.NULL_CHAR

    var i = encodedLengthStrLen + 1 // skip 'encodedLength="'
    var foundValue = false
    var encLen = 0L // TODO: Long value?

    while (i < 128 && !foundValue) {
      val c = encodedLengthChunk(i)
      //encodedLengthAsStr(i) = c
      if (c == '"') {
        foundValue = true
      } else {
        assert(isdigit(c) != 0, "invalid digit: " + c.toChar)
        encLen *= 10
        encLen += c - CUtils.ZERO_ASCII_CODE
      }

      i += 1
    }

    // Search for '<cvParam' section
    var currentChunk = binaryDataArrayMetaDataAsCString
    val accessions = new scala.collection.mutable.ArrayBuffer[String]()

    var parsedParams = false
    while (!parsedParams) {
      val cvParamsChunk = strstr(currentChunk, cvParamCStr)
      /*assert(
        cvParamsChunk != null,
        s"can't parse cvParams in spectrum chunk with id = '$id'"
      )*/

      if (cvParamsChunk == null) parsedParams = true
      else {
        val cvParamAc = _scanCvParamAccession(cvParamsChunk, 256)
        //println("cvParamAc: " + cvParamAc)
        accessions += cvParamAc
        currentChunk = cvParamsChunk + 10 // skip '<cvParam' tag to match the next one
      }
    }

    // FIXME: we don't need a full Map on the list of accession names
    MzMLBinaryChunkMetaData(encLen.toULong, accessions.map(ac => ac -> ("","")) )

    /*
    // Alternative method (DBO):

    val binaryDataArrayMetaData = fromCString(cvParamsChunk)
    val cvParamsAsStr = binaryDataArrayMetaData.split("/>").dropRight(1)
    //println(cvParamsAsStr.length)

    // TODO: parse name and value
    val cvParams = for (cvParamAsStr <- cvParamsAsStr) yield {
      val accession = cvParamAsStr.split("accession=\"")(1).takeWhile(_ != '"')
      (accession -> ("",""))
    }

    // Algo which can be used to scan a string


    MzMLBinaryChunkMetaData(encLen, cvParams.toSeq)*/
  }

  private def _scanCvParamAccession(cvParamChunk: CString, maxLen: CInt): String = {

    val accessionCStr = c"""accession="""
    val accessionStrLen = 10

    val accessionChunk = strstr(cvParamChunk, accessionCStr)
    assert(
      accessionChunk != null,
      s"can't find cvParam 'accession' in spectrum chunk with id = '$id': ${fromCString(cvParamChunk).take(200)}"
    )

    var j = 0
    var i = accessionStrLen + 1 // skip 'accession="'
    var foundValue = false
    val cvParamAC: CString = stackalloc[CChar]((10 + 1).toULong)
    cvParamAC(10.toULong) = CUtils.NULL_CHAR

    while (i < maxLen && !foundValue) {
      val c = accessionChunk(i)
      //encodedLengthAsStr(i) = c
      if (c == '"') {
        foundValue = true
      } else {
        cvParamAC(j) = c
      }

      i += 1
      j += 1
    }

    //println("cvParamAC: " + fromCString(cvParamAC))

    fromCString(cvParamAC)

    /*var i = 0
    var charSum = 0
    var nChars = 0
    var foundValue = false

    while (i < 256 && !foundValue) {
      val c: CChar = cvParamChunk(i)
      charSum += c

      if (nChars == encodedLengthStrLen) {
        println("charSum: " + charSum)
        if (charSum == charSum) {
          foundValue = true
        } else {
          val charToRemove = cvParamChunk(i - strLen)
          println("char: " + charToRemove.toChar)
          charSum -= charToRemove
        }
      } else {
        nChars += 1
      }

      i += 1
    }*/
  }

  /*private def _parse_binary_chunk_meta_data_slow(binaryDataArrayXmlChunk: CString): MzMLBinaryChunkMetaData = {

    // Search for the encodedLength of the first <binaryDataArray> element
    val binaryDataArrayMetaDataAsCString: CString = stackalloc[CChar](1024)
    CUtils.strcpy(binaryDataArrayMetaDataAsCString, binaryDataArrayXmlChunk, 1023)

    val binaryDataArrayMetaData = fromCString(binaryDataArrayMetaDataAsCString)

    val encodedLength: CSize = binaryDataArrayMetaData match {
      case MzMLSpectrumChunk.encodedLengthRegex(length) => length.toInt
      case _ => throw new Exception(s"can't find binaryDataArray encodedLength in spectrum chunk with id = '$id'")
    }

    //println(binaryDataArrayMetaData)

    val cvParams = for (
      MzMLSpectrumChunk.cvParamRegex(ac,name,value) <- cvParamRegex findAllIn binaryDataArrayMetaData
    ) yield ac -> (name, value)

    assert(cvParams.nonEmpty, s"can't parse cvParams from binary chunk of spectrum with id = '$id'")
    //val cvParamByAc = cvParams.toMap
    //println(cvParamByAc)

    MzMLBinaryChunkMetaData(encodedLength, cvParams.toSeq)
  }*/

  private def _parse_vector_from_binary_chunk(binaryXmlChunk: CString, metaData: MzMLBinaryChunkMetaData): PtrBox = {

    /*val str = fromCString(binaryXmlChunk)
    val head = str.take(100)
    val tail = str.takeRight(100)
    println(s"binary chunk head/tail: \n$head\n$tail")*/

    val encodedLength = metaData.encodedLength

    // Note: we need to calloc 'encodedLength + 1' to allocate space for the NUL terminator
    val base64_str: CString = calloc( encodedLength + 1L.toULong, 1L.toULong)
    assert(base64_str != null, "_parse_vector_from_binary_chunk: can't allocate memory")

    // Copy Base64 encoded string to a specific allocated string
    memcpy( base64_str, binaryXmlChunk, encodedLength ) // returns base64_str

    // Ensure that NULL terminator is well positioned
    //base64_str(encodedLength) = CUtils.NULL_CHAR

    //val len = strlen(base64_str)
    //println("str len: " + len)
    //println("encodedLength: " + encodedLength)
    //println("["+fromCString(base64_str)+"]")
    //println("read length: " + fromCString(base64_str).length)

    // Decode Base64 string
    val vector = _decode_base64_vector(base64_str)

    // Free Base64 string
    free(base64_str)

    vector
  }

  /*private def _strncpy(dest: CString, src: CString, len: CSize): Unit = {
    var i: CInt = 0
    while (i < len) {
      dest(i) = src(i)
      i += 1
    }
  }*/

  private def _decode_base64_vector(b64_str: CString): PtrBox = {
    // TODO: if we parse the "defaultArrayLength" value from the spectrum attributes, we could skip the step "calcDecodedStrLen"
    val decodedStrLen: CSize = Base64Lib.calcDecodedStrLen(b64_str).toULong
    //println("expected decodedStrLen: " + decodedStrLen)

    //val vector = calloc(decodedStrLen, 1L)
    val vector = alloc[Byte](decodedStrLen) // caller managed memory
    assert(vector != null, "_decode_base64_vector: can't allocate memory")

    val actualSize: CSize = Base64Lib.decode(vector, b64_str).toULong
    //println("actualSize: " + actualSize)

    PtrBox(vector, actualSize)
  }

  /*def destroy(): Unit = {
    // Detach XML String from the chunk object and free memory
    if (mzArrayMetaData != null) mzArrayMetaData = null
    if (intensityArrayMetaData != null) intensityArrayMetaData = null
    if (spectrumHeaderChunk != null) spectrumHeaderChunk = null
  }*/
}