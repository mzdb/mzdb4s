package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

import com.github.mzdb4s.util.atomic.InMemoryIdGen

case class RunSliceData(
  @BeanProperty val id: Int,
  @BeanProperty val spectrumSliceList: Array[SpectrumSlice]
) extends InMemoryIdGen