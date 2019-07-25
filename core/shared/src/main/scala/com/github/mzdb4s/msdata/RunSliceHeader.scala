package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

import com.github.mzdb4s.util.atomic.InMemoryIdGen

case class RunSliceHeader(
  @BeanProperty id: Int,
  @BeanProperty msLevel: Int,
  @BeanProperty number: Int,
  @BeanProperty beginMz: Double,
  @BeanProperty endMz: Double,
  @BeanProperty runId: Int
) extends InMemoryIdGen with Comparable[RunSliceHeader] {

  override def compareTo(o: RunSliceHeader): Int = {
    if (beginMz < o.beginMz) -1
    else if (Math.abs(beginMz - o.beginMz) < 1e-6) 0
    else 1
  }

}