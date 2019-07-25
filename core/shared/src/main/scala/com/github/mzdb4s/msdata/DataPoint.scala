package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

case class DataPoint(
  @BeanProperty x: Double,
  @BeanProperty y: Int
)
