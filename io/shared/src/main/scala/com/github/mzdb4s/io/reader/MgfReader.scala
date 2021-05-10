package com.github.mzdb4s.io.reader

import java.io.{BufferedReader, File, FileReader}
import scala.collection.mutable.ArrayBuffer
import com.github.mzdb4s.io.mgf._

object MgfReader {
  private val DEFAULT_BUFFER_SIZE = 64 * 1024 // 64 KB
}

class MgfReader(val mgfFile: File, bufferSize: Int) {
  require(mgfFile.isFile, s"can't find an MGF file at '$mgfFile''")

  def this(mgfFileLocation: String, bufferSize: Int = MgfReader.DEFAULT_BUFFER_SIZE) {
    this(new File(mgfFileLocation), bufferSize)
  }

  def foreachMgfSpectrum(spectrum: MgfSpectrum => Boolean): Unit = {

    val bufferedReader = new BufferedReader(new FileReader(mgfFile), bufferSize)
    try {
      val lineBuffer = new ArrayBuffer[String](10 * 1024)
      var inSpectrumBlock = false
      var continue = true

      var line: String = null
      while ({line = bufferedReader.readLine; continue && line != null}) {

        if (line.startsWith("BEGIN IONS")) {
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
      if (line.contains('=')) {
        val lineParts = line.split('=')
        mgfHeaderEntries += MgfHeaderEntry(MgfField.withName(lineParts.head),lineParts.tail.mkString("="))
      } else {
        val lineParts = line.split("\\s")
        mzList += lineParts(0).toDouble
        intensityList += lineParts(1).toFloat
      }
    }

    MgfSpectrum(MgfHeader(mgfHeaderEntries), mzList.toArray, intensityList.toArray)
  }
}

