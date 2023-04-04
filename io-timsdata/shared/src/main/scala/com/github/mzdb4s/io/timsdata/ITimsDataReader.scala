package com.github.mzdb4s.io.timsdata

case class TimsDataReaderConfig(
  ms1Only: Boolean,
  mzTolPPM: Int,
  minDataPointsCount: Int,
  nThreads: Int,
  nTimsdataDllThreads: Int,
)

trait ITimsDataReader {

  type ForEachMergedSpectrumCb = (Long, Int, Int, Array[Double], Array[Float]) => Unit

  //def forEachMergedSpectrum(timsDataDir: String, onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit

  def forEachMergedSpectrum(timsDataDir: String, config: Option[TimsDataReaderConfig], onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit

}
