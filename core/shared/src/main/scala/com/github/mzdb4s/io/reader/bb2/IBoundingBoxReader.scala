package com.github.mzdb4s.io.reader.bb2

import scala.collection.mutable.Buffer
import scala.collection.mutable.WrappedArray

import com.github.mzdb4s.msdata.SpectrumSlice
import com.github.mzdb4s.msdata.builder._

/**
  * Trait for reading SQLite Blob
  *
  * @author David Bouyssie
  *
  */
trait IBoundingBoxReader {

  def bbId: Int
  def runSliceId: Int
  def firstSpectrumId: Long
  def lastSpectrumId: Long

  /**
    * Cleanup the resources
    */
  def dispose(): Unit

  /**
    * @return the spectra count in the blob.
    */
  def getSpectraCount(): Int

  /**
    * @param idx index of spectrum starting at 0
    * @return long, the ID of the spectrum at the specified index in the blob
    */
  def getSpectrumIdAt(idx: Int): Long

  def getPeaksCountAt(idx: Int): Int

  /**
    * @return the ids of all spectra contained in the bounding box
    */
  def getAllSpectraIds(spectraIds: WrappedArray[Long]): Unit

  def readSpectrumSliceDataAt(idx: Int, builder: ISpectrumDataAdder): Unit

  def readFilteredSpectrumSliceDataAt(idx: Int, minMz: Double, maxMz: Double, builder: ISpectrumDataAdder): Unit

  def readSpectrumSliceAt(idx: Int)(implicit sdbFactory: SpectrumDataBuilderFactory): SpectrumSlice

  def readAllSpectrumSlicesData(builders: Seq[ISpectrumDataAdder]): Unit

  def readAllSpectrumSlices(buffer: Buffer[SpectrumSlice])(implicit sdbFactory: SpectrumDataBuilderFactory): Unit
}

trait IByteBufferAsInts extends Any {
  def apply(bytesIndex: Int)(implicit bbIdxFactory: BoundingBoxIndexFactory): Int
}
