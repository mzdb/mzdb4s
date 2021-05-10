package com.github.mzdb4s.io.mgf

case class MgfSpectrum(
  mgfHeader: MgfHeader,
  mzList: Array[Double],
  intensityList: Array[Float]
)