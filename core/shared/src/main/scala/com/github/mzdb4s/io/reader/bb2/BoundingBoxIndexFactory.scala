package com.github.mzdb4s.io.reader.bb2

import com.github.mzdb4s.msdata.DataEncoding

import scala.collection.mutable.{HashSet, LongMap}

// TODO: rename this class since we also cache a buffer for the peaks data
private[reader] class BoundingBoxIndexFactory() {

  private val _intBuffer = new Array[Byte](4)
  private val _releasedIndexes = new HashSet[BoundingBoxIndex]

  private var _peaksBuffer: Array[Byte] = null

  def reusableIntBuffer: Array[Byte] = _intBuffer

  def acquirePeaksBuffer(minSize: Int): Array[Byte] = {
    if (_peaksBuffer == null || _peaksBuffer.length < minSize) {
      _peaksBuffer = new Array[Byte](minSize)
    }

    _peaksBuffer
  }

  def acquireIndex(dataEncodingBySpectrumId: LongMap[DataEncoding]): BoundingBoxIndex = {
    implicit val bbIdxFactory: BoundingBoxIndexFactory = this

    val releasedIndexOpt = _releasedIndexes.headOption
    if (releasedIndexOpt.isEmpty) new BoundingBoxIndex(dataEncodingBySpectrumId)
    else {
      val releasedIndex = releasedIndexOpt.get
      _releasedIndexes -= releasedIndex
      //println(s"Re-using previously created index")
      releasedIndex
    }
  }

  def releaseIndex(index: BoundingBoxIndex): Unit = {
    _releasedIndexes += index
  }

}
