package com.github.mzdb4s.io.timsdata

trait ITimsDataReader {

  type ForEachMergedSpectrumCb = (Long, Int, Int, Array[Double], Array[Float]) => Unit

  def forEachMergedSpectrum(timsDataDir: String, onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit

}
