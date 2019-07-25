package com.github.mzdb4s.msdata.builder

import scala.collection.mutable.HashSet

class SpectrumDataBuilderFactory() {

  private val _releasedBuilders = new HashSet[SpectrumDataBuilder]

  def acquireBuilder(minDataPointsCount: Int): SpectrumDataBuilder = {
   val releasedBuilderOpt = _releasedBuilders.find(_.dataPointsCount >= minDataPointsCount)
    if (releasedBuilderOpt.isEmpty) new SpectrumDataBuilder(minDataPointsCount)
    else {
      val releasedBuilder = releasedBuilderOpt.get
      _releasedBuilders -= releasedBuilder
      //println(s"Re-using previously created builder : minDataPointsCount=$minDataPointsCount ; dataPointsCount = ${releasedBuilder.dataPointsCount}")
      releasedBuilder
    }
  }

  def releaseBuilder(builder: SpectrumDataBuilder): Unit = {
    _releasedBuilders += builder
  }

}
