package com.github.mzdb4s.io.reader

import java.io.{BufferedReader, File, FileReader}
import scala.collection.mutable.ArrayBuffer
import com.github.mzdb4s.io.mgf._
import com.github.mzdb4s.util.lang.String._

object MgfReader {
  private val DEFAULT_BUFFER_SIZE = 64 * 1024 // 64 KB
  private val DEFAULT_PEAK_SEPARATOR = ' '
}

class MgfReader(
  val mgfFile: File,
  bufferSize: Int = MgfReader.DEFAULT_BUFFER_SIZE,
  peakSeparator: Char = MgfReader.DEFAULT_PEAK_SEPARATOR
) {
  require(mgfFile.isFile, s"can't find an MGF file at '$mgfFile''")

  def this(mgfFileLocation: String) {
    this(new File(mgfFileLocation))
  }

  def foreachMgfSpectrum(spectrum: MgfSpectrum => Boolean): Unit = {

    val bufferedReader = new BufferedReader(new FileReader(mgfFile), bufferSize)
    try {
      val lineBuffer = new ArrayBuffer[String](10 * 1024)
      var inSpectrumBlock = false
      var continue = true

      while (continue) {
        val line = bufferedReader.readLine

        if (line == null) {
          continue = false
        } else if (line.startsWith("BEGIN IONS")) {
          inSpectrumBlock = true
        }
        else if (line.startsWith("END IONS")) {
          val mgfSpectrum = _textBlockToMgfSpectrum(lineBuffer)
          continue = spectrum(mgfSpectrum)
          lineBuffer.clear()
          inSpectrumBlock = false
        }
        else if (inSpectrumBlock) {
          lineBuffer += line
        }
      }
    } finally {
      bufferedReader.close()
    }
  }

  private def _textBlockToMgfSpectrum(textBlock: ArrayBuffer[String]): MgfSpectrum = {

    val mgfHeaderEntries = new ArrayBuffer[MgfHeaderEntry]()
    val mzList = new ArrayBuffer[Double](textBlock.length)
    val intensityList = new ArrayBuffer[Float](textBlock.length)

    for (line <- textBlock; if line.nonEmpty && line.charAt(0) != '#') {
      val metaDataParts = line.lazySplit('=')
      if (metaDataParts.nonEmpty) {
        val knownFieldOpt = MgfField.maybeNamed(metaDataParts.head)
        if (knownFieldOpt.isDefined) {
          mgfHeaderEntries += MgfHeaderEntry(knownFieldOpt.get,metaDataParts.tail.mkString("="))
        }
      } else {
        val peakParts = line.lazySplit( peakSeparator)
        if (peakParts.nonEmpty) {
          mzList += peakParts(0).toDouble
          intensityList += peakParts(1).toFloat
        }
      }
    }

    /*println(mgfHeaderEntries.toList)
    println(mzList.toList)
    println(intensityList.toList)*/

    MgfSpectrum(MgfHeader(mgfHeaderEntries), mzList.toArray, intensityList.toArray)
  }

  def foreachMgfHeader(spectrum: MgfHeader => Boolean): Unit = {

    val bufferedReader = new BufferedReader(new FileReader(mgfFile), bufferSize)
    try {
      val mgfHeaderEntries = new ArrayBuffer[MgfHeaderEntry]()

      var inSpectrumBlock = false
      var continue = true

      while (continue) {
        val line = bufferedReader.readLine

        if (line == null) {
          continue = false
        } else if (line.startsWith("BEGIN IONS")) {
          inSpectrumBlock = true
        }
        else if (line.startsWith("END IONS")) {
          continue = spectrum(MgfHeader(mgfHeaderEntries.clone()))
          mgfHeaderEntries.clear()
          inSpectrumBlock = false
        }
        // Filter header lines
        else if (inSpectrumBlock && line.nonEmpty && !Character.isDigit(line.charAt(0)) && line.charAt(0) != '#') {
          val metaDataParts = line.lazySplit('=')
          if (metaDataParts.nonEmpty) {
            val knownFieldOpt = MgfField.maybeNamed(metaDataParts.head)
            if (knownFieldOpt.isDefined) {
              mgfHeaderEntries += MgfHeaderEntry(knownFieldOpt.get,metaDataParts.tail.mkString("="))
            }
          }
        }
      }
    } finally {
      bufferedReader.close()
    }
  }

}

