package com.github.mzdb4s.io.writer

import java.io.File

import com.github.mzdb4s.MzDbReader
import com.github.mzdb4s.msdata._

import scala.scalanative.libc.string
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._
import com.github.utils4sn._

class MgfSpectrumSerializer() extends AbstractMgfSpectrumSerializer {

  private val LINE_SEPARATOR_CSTR: CString = c"\n"
  private val LINE_SEPARATOR_LEN = 1

  private val END_IONS_CSTR: CString = c"END IONS"
  private val END_IONS_LEN = 8

  /*
  val strBuilder = com.github.utils4sn.CStringBuilder.create()
      numbers foreach { d =>

        //java.lang.String.format("%.3f", d.asInstanceOf[java.lang.Double])
        //val str = f"$d%.3f"
        //val str = s"${round(d,3)} ${round(d,1)}"

        val cstr = stackalloc[CChar](12)
        val len = com.github.utils4sn.bindings.StrBuilderLib.dtoa(d, cstr, 3)
        //val str = fromCString(cstr)

        strBuilder.addString(cstr, len)

        /*val str2 = s"${round(d,3)}"
        strBuilder.append(str2)*/
        /*assert(str == str2, s"$str != $str2")*/

        //val str = df.format(d)

        /*Zone { implicit z =>
          val cStr = stackalloc[CChar](16)
          scala.scalanative.libc.stdio.vsprintf(cStr, c"%.3f %.1f", toCVarArgList(CVarArg.materialize(d), CVarArg.materialize(d)))

          val str = fromCString(cStr)
        }*/

      }

      fromCString(strBuilder.underlyingString())

      strBuilder.destroy()

   */

  private val spectrumStringBuilder: CStringBuilderInstance = CStringBuilder.create()

  protected def disposeStringBuilder(): Unit = {
    spectrumStringBuilder.destroy()
    //spectrumStringBuilder = null
  }

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
  ): Array[Byte] = {

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

    Zone { implicit z =>
      val mgfHeaderAsStr = mgfSpectrumHeader.toString()
      spectrumStringBuilder.addString(toCString(mgfHeaderAsStr), mgfHeaderAsStr.length.toULong)
    }

    // Spectrum Data
    val data = spectrum.getData
    val mzs = data.mzList
    val ints = data.intensityList
    val intsLength = ints.length

    if (intensityCutoff.isEmpty) {
      var i = 0
      while (i < intsLength) {
        appendPeak(mzs(i), ints(i), mzFragDecimals)
        i += 1
      }
    } else {
      val cutoff = intensityCutoff.get
      var i = 0
      while (i < intsLength) {
        val intensity = ints(i)
        if (intensity >= cutoff) {
          appendPeak(mzs(i), intensity, mzFragDecimals)
        }
        i += 1
      }
    }

    val spectrumAsCStr = spectrumStringBuilder
      .addString(END_IONS_CSTR, END_IONS_LEN.toULong)
      .addString(LINE_SEPARATOR_CSTR, LINE_SEPARATOR_LEN.toULong)
      .underlyingString()

    //fromCString(spectrumAsCStr)

    val bytes: Array[Byte] = com.github.sqlite4s.c.util.CUtils.bytes2ByteArray(
      spectrumAsCStr,
      scala.scalanative.libc.string.strlen(spectrumAsCStr)
    )

    bytes
  }

  @inline
  def appendPeak(mz: Double, intensity: Float, mzFragDecimals: Int): Unit = {
    val mzCStr = stackalloc[CChar](24.toULong)
    val mzStrLen = com.github.utils4sn.bindings.StrBuilderLib.dtoa(mz, mzCStr, mzFragDecimals)

    //println("mzCStr: " + fromCString(mzCStr))
    //assert(mzStrLen == 0, "mzStrLen: " + mzStrLen)

    val intCStr = stackalloc[CChar](24.toULong)
    val intStrLen = com.github.utils4sn.bindings.StrBuilderLib.ftoa(intensity, intCStr, 0)

    spectrumStringBuilder
      .addString(mzCStr, mzStrLen.toULong)
      .addChar(32.asInstanceOf[CChar]) // add space
      .addString(intCStr, intStrLen.toULong)
      .addString(LINE_SEPARATOR_CSTR, LINE_SEPARATOR_LEN.toULong)
  }

}
