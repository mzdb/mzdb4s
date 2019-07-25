package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

case class RunSlice(
  @BeanProperty val header: RunSliceHeader,
  @BeanProperty val data: RunSliceData
)