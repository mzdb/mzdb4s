/*package com.github.mzdb4s.io.reader.bb2

import scala.beans.BeanProperty
import com.github.mzdb4s.msdata._

import scala.collection.mutable.ArrayBuffer

// TODO: DBO => move this class to package com.github.mzdb4s.msdata
case class BoundingBox private[this] (
  @BeanProperty var id: Int,
  @BeanProperty var firstSpectrumId: Long,
  @BeanProperty var lastSpectrumId: Long,
  @BeanProperty var runSliceId: Int,
  @BeanProperty var msLevel: Int,
  @BeanProperty var dataMode: DataMode.Value,
  @BeanProperty var spectrumSlices: ArrayBuffer[SpectrumSlice]
) extends Comparable[BoundingBox] {

  spectrumSlices.reduceToSize()
  override def compareTo(anotherBB: BoundingBox): Int = {
    java.lang.Long.compare(this.firstSpectrumId, anotherBB.firstSpectrumId)
  }
}

class BoundingBoxPool() {

  def acquireBoundingBox(): Unit = {

  }

}*/