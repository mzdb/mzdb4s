package com.github.mzdb4s.msdata

import com.github.mzdb4s.msdata.builder.SpectrumSliceBuilder

import scala.beans.BeanProperty

import com.github.mzdb4s.io.reader.bb.IBlobReader

// TODO: DBO => move this class to package com.github.mzdb4s.io.reader.bb ???
class BoundingBox(
  @BeanProperty var id: Int,
  @BeanProperty val reader: IBlobReader
) extends Comparable[BoundingBox] {

  @BeanProperty var firstSpectrumId = 0L
  @BeanProperty var lastSpectrumId = 0L
  @BeanProperty var runSliceId = 0
  protected var _msLevel = 0
  protected var _dataMode: DataMode.Value = _

  def getSpectraCount(): Int = reader.getSpectraCount()

  def getMinSpectrumId(): Long = this.reader.getSpectrumIdAt(0)

  def getMaxSpectrumId: Long = this.reader.getSpectrumIdAt(this.getSpectraCount - 1)

  def toSpectrumSlices(): Array[SpectrumSlice] = {
    reader.readAllSpectrumSlices(this.runSliceId)

    // FIXME: remove this workaround when raw2mzDB has been fixed
    // raw2mzDB is inserting multiple empty spectrum slices pointing to the same spectrum id
    // Workaround added the 22/01/2015 by DBO
    /*val spectrumIdSet = new util.HashSet[Long]
    for (spectrumSlice <- spectrumSliceArray) {
      val spectrumId = spectrumSlice.getHeader.getId
      if (spectrumIdSet.contains(spectrumId) == true) throw new IllegalArgumentException("duplicated spectrum id is: " + spectrumId)
      spectrumIdSet.add(spectrumId)
    }
    spectrumSliceArray*/
  }

  /*def toSpectrumSliceBuilders(): Array[SpectrumSliceBuilder] = {
    reader.readAllSpectrumSlices(this.runSliceId).map { ss =>
      val data = ss.getData
      new SpectrumSliceBuilder(ss.header, data.peaksCount).addSpectrumData(data)
    }
  }*/

  override def compareTo(bb: BoundingBox): Int = {
    if (this.getMinSpectrumId < bb.getMinSpectrumId) return -1
    else if (Math.abs(this.getMinSpectrumId - bb.getMinSpectrumId) == 0) return 0
    1
  }
}

/**
  * Class holding bounding box dimensions that retrieved from the param_tree of the mzdb table.
  * We distinguish two sizes, one for ms1, the other one for all MSn.
  */
case class BBSizes(
  BB_MZ_HEIGHT_MS1: Double,
  BB_MZ_HEIGHT_MSn: Double,
  BB_RT_WIDTH_MS1: Float,
  BB_RT_WIDTH_MSn: Float
)