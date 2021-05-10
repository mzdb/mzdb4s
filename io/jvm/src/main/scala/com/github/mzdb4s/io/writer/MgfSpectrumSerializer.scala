package com.github.mzdb4s.io.writer

import java.io.File

import com.github.mzdb4s.MzDbReader
import com.github.mzdb4s.io.mgf._
import com.github.mzdb4s.msdata._

class MgfSpectrumSerializer() extends AbstractMgfSpectrumSerializer {

  private var spectrumStringBuilder: StringBuilder = new StringBuilder(1024*1024)

  protected def disposeStringBuilder(): Unit = {
    spectrumStringBuilder.clear()
    spectrumStringBuilder = null
  }

/*
  protected def clearStringBuilder(): Unit = spectrumStringBuilder.clear()
  protected def stringBuilderToString(): String = spectrumStringBuilder.toString()
  */

  def stringifySpectrum(
    mzDBFilePath: String,
    mzDbReader: MzDbReader,
    spectrum: Spectrum,
    titleBySpectrumId: collection.Map[Long,String],
    dataEnc: DataEncoding,
    precMz: Double,
    charge: Int,
    intensityCutoff: Option[Float],
    exportProlineTitle: Boolean
  ): String = {

    spectrumStringBuilder.clear()

    // FIXME: check if is_high_res parameter is used and is correct
    val mzFragDecimals = if (dataEnc.getPeakEncoding == PeakEncoding.LOW_RES_PEAK) 1 else 3

    // Unpack data
    val spectrumHeader = spectrum.getHeader
    val id = spectrumHeader.getInitialId
    val time = spectrumHeader.getElutionTime()

    val title = if (!exportProlineTitle) titleBySpectrumId(spectrumHeader.getSpectrumId())
    else {
      val timeInMinutes = MathUtils.round(time / 60,4)

      val cycle = spectrumHeader.getCycle
      val rawFile = mzDbReader.getFirstSourceFileName().split('.').headOption.getOrElse {
        new File(mzDBFilePath).getName.split('.').headOption.getOrElse("")
      }

      s"first_cycle:$cycle;last_cycle:$cycle;first_scan:$id;last_scan:$id;first_time:$timeInMinutes;last_time:$timeInMinutes;raw_file_identifier:$rawFile;"
    }

    val mgfSpectrumHeader = if (charge != 0) MgfWriter.createMgfHeader(title, precMz, charge, time, id)
    else MgfWriter.createMgfHeader(title, precMz, time, id)

    mgfSpectrumHeader.appendToStringBuilder(spectrumStringBuilder)

    // Spectrum Data
    val data = spectrum.getData
    val mzs = data.mzList
    val ints = data.intensityList
    val intsLength = ints.length

    @inline def appendPeak(mz: Double, intensity: Float): Unit = {
      //MgfWriter.round(mz,mzFragDecimals)
      //(java.lang.Math.round(mz * 1000).toDouble / 1000).toString
      //BigDecimal(mz).setScale(3, scala.math.BigDecimal.RoundingMode.HALF_UP).toString
      //java.lang.Math.round(intensity)
      spectrumStringBuilder
        .append(MathUtils.round(mz,mzFragDecimals))
        .append(" ")
        .append(math.round(intensity))
        .append(MgfWriter.LINE_SEPARATOR)
    }

    if (intensityCutoff.isEmpty) {
      var i = 0
      while (i < intsLength) {
        appendPeak(mzs(i), ints(i))
        i += 1
      }
    } else {
      val cutoff = intensityCutoff.get
      var i = 0
      while (i < intsLength) {
        val intensity = ints(i)
        if (intensity >= cutoff) {
          appendPeak(mzs(i), intensity)
        }
        i += 1
      }
    }

    spectrumStringBuilder.append(MgfField.END_IONS)
    spectrumStringBuilder.append(MgfWriter.LINE_SEPARATOR)
    spectrumStringBuilder.toString
  }

}
