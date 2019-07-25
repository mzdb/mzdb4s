package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

case class Chromatogram(
  /** The data points. */
  @BeanProperty dataPoints: Array[DataPoint]
)