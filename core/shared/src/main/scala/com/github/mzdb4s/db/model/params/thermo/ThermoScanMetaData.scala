package com.github.mzdb4s.db.model.params.thermo

import java.util.regex.Pattern

import scala.beans.BeanProperty

object ThermoScanMetaData {

  /*"FTMS + p NSI sps d Full ms3 707.8472@cid35.00 463.3669@hcd45.00 [115.0000-140.0000]"
    ITMS + c NSI d Full ms2 476.20@cid30.00 [120.00-1440.00]

      spectrum_type: "FTMS + p NSI sps d Full ms3",
      analyzer_type: "FTMS",
      ms_level : 3,
      mz_range: [115.0000,140.0000],
      targets: [
        {ms_level: 1, mz: 707.8472, activation_type: 'CID', collision_energy: 35},
        {ms_level: 2, mz: 463.3669, activation_type: 'HCD', collision_energy: 45}
        ]
  */

  // MS2 example: ITMS + c NSI d Full ms2 476.20@cid30.00 [120.00-1440.00]
  // MS3 example: FTMS + p NSI sps d Full ms3 707.8472@cid35.00 463.3669@hcd45.00 [115.0000-140.0000]
  private[thermo] val targetPattern = "\\s(\\d+\\.\\d+)@([a-z]+)(\\d+\\.\\d+)"
  private[thermo] val ms2Pattern = "\\d" + targetPattern + "\\s\\[(\\d+\\.\\d+)-(\\d+\\.\\d+)\\]"
  private[thermo] val ms3Pattern = "\\d" + targetPattern + targetPattern + "\\s\\[(\\d+\\.\\d+)-(\\d+\\.\\d+)\\]"

  def apply(filterString: String): ThermoScanMetaData = {

    val stringParts = filterString.split("Full ms")
    val rightString = stringParts(1)
    val msLevelChar = rightString.charAt(0)
    val msLevel = Character.getNumericValue(msLevelChar)

    val spectrumType = stringParts(0) + "Full ms" + msLevelChar

    val pattern = if (msLevel == 3) ms3Pattern else ms2Pattern

    // Compile the regex
    val r = Pattern.compile(pattern)
    val m = r.matcher(rightString)

    val targets = new Array[ThermoFragmentationTarget](msLevel - 1)
    val mzRange = Array(0f, 0f)

    if (m.find) { // Parse MS2 target information
      targets(0) = ThermoFragmentationTarget(1, m.group(1).toDouble, m.group(2), m.group(3).toFloat)
      if (msLevel == 2) {
        mzRange(0) = m.group(4).toFloat
        mzRange(1) = m.group(5).toFloat
      }
      else { // Parse MS3 target information
        targets(1) = ThermoFragmentationTarget(2, m.group(4).toDouble, m.group(5), m.group(6).toFloat)
        mzRange(0) = m.group(7).toFloat
        mzRange(1) = m.group(8).toFloat
      }
    }

    ThermoScanMetaData(spectrumType, stringParts(0).split("\\s")(0), msLevel, mzRange, targets)
  }
}

case class ThermoScanMetaData(
  @BeanProperty spectrumType: String,
  @BeanProperty analyzerType: String,
  @BeanProperty msLevel: Int,
  @BeanProperty mzRange: Array[Float],
  @BeanProperty targets: Array[ThermoFragmentationTarget]
)

/*class ThermoScanMetaData {

  /** The spectrumType. */
  final protected var acquisitionType: String = null
  /** The analyzerType. */
  final protected var analyzerType: String = null
  /** The msLevel. */
  protected var msLevel = 0
  /** The mzRange. */
  final protected var mzRange: Array[Float] = null
  /** The targets. */
  final protected var targets: Array[ThermoFragmentationTarget] = null

  def this(spectrumType: String, analyzerType: String, msLevel: Int, mzRange: Array[Float], targets: Array[ThermoFragmentationTarget]) {
    this()
    this.acquisitionType = spectrumType
    this.analyzerType = analyzerType
    this.msLevel = msLevel
    this.mzRange = mzRange
    this.targets = targets
  }

  def getAcquisitionType: String = acquisitionType

  def getAnalyzerType: String = analyzerType

  def getMsLevel: Int = msLevel

  def getMzRange: Array[Float] = mzRange

  def getTargets: Array[ThermoFragmentationTarget] = targets
}*/