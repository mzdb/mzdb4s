package com.github.mzdb4s.io.writer

import java.io.File

import com.github.mzdb4s.MzDbReader
import com.github.mzdb4s.msdata._

abstract class AbstractMgfSpectrumSerializer {

  protected def disposeStringBuilder(): Unit
  def dispose(): Unit = this.disposeStringBuilder()

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
  ): Array[Byte]

  /*protected def initStringBuilder(): Unit
  protected def clearStringBuilder(): Unit
  protected def stringBuilderToString(): String
  protected def disposeStringBuilder(): Unit*/


}
