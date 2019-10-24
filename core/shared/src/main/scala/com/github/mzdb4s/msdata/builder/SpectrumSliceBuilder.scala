package com.github.mzdb4s.msdata.builder

import com.github.mzdb4s.msdata._

class SpectrumSliceBuilder(
  val header: SpectrumHeader,
  val runSliceId: Int,
  val spectrumDataBuilder: ISpectrumDataBuilder
) extends AbstractSpectrumBuilder {

  def this(header: SpectrumHeader, runSliceId: Int, initialDataSize: Int)(implicit sdbFactory: SpectrumDataBuilderFactory) {
    this(header, runSliceId, sdbFactory.acquireBuilder(initialDataSize))
  }

  def result(): SpectrumSlice = {
    val slice = SpectrumSlice(header, spectrumDataBuilder.result())
    slice.setRunSliceId(runSliceId)
    slice
  }
}