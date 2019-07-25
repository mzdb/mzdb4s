package com.github.mzdb4s.io.reader.cache

import scala.collection.mutable.LongMap
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.msdata.{BBSizes, RunSliceHeader}

class RunSliceHeaderReader(
  bbSizes: BBSizes,
  entityCache: Option[MzDbEntityCache] = None
)(implicit val mzDbCtx: MzDbContext) extends AbstractRunSliceHeaderReader(bbSizes, entityCache) {

  /** Proxy methods **/

  def getRunSliceHeaders(): Array[RunSliceHeader] = super.getRunSliceHeaders()

  def getRunSliceHeaders(msLevel: Int): Array[RunSliceHeader] = super.getRunSliceHeaders(msLevel)

  def getRunSliceHeaderById(msLevel: Int): LongMap[RunSliceHeader] = super.getRunSliceHeaderById(msLevel)

  def getRunSliceHeaderById: LongMap[RunSliceHeader] = super.getRunSliceHeaderById()

  def getRunSliceHeader(id: Int): RunSliceHeader = super.getRunSliceHeader(id)

  def getRunSliceHeaderForMz(mz: Double, msLevel: Int): RunSliceHeader = super.getRunSliceHeaderForMz(mz, msLevel)

  def getRunSliceIdsForMzRange(minMz: Double, maxMz: Double, msLevel: Int): Array[Int] = {
    super.getRunSliceIdsForMzRange(minMz, maxMz, msLevel)
  }
}