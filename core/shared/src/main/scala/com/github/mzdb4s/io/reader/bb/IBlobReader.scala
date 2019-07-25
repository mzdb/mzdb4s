package com.github.mzdb4s.io.reader.bb

import com.github.mzdb4s.msdata._

/**
  * Trait for reading SQLite Blob
  *
  * @author David Bouyssie
  *
  */
trait IBlobReader {

  /**
    * Cleanup the blob if necessary
    */
  def disposeBlob(): Unit

  /**
    * @return the spectra count in the blob.
    */
  def getSpectraCount(): Int

  /**
    * @param i index of spectrum starting at 0
    * @return long, the ID of the spectrum at the specified index in the blob
    */
  def getSpectrumIdAt(i: Int): Long

  def getAllSpectrumIds(): Array[Long]

  /**
    *
    * @param runSliceId needed to correctly annotate the SpectrumSlice
    * @return array of spectrumSlice representing the bounding box
    */
  def readAllSpectrumSlices(runSliceId: Int): Array[SpectrumSlice]

  /**
    *
    * @param idx index of specified spectrum
    * @return SpectrumSlice of the specified spectrum
    */
  def readSpectrumSliceAt(idx: Int): SpectrumSlice

  /**
    *
    * @param idx index of specified spectrum
    * @return SpectrumData of the specified spectrum
    */
  def readSpectrumSliceDataAt(idx: Int): ISpectrumData

  def readFilteredSpectrumSliceDataAt(idx: Int, minMz: Double, maxMz: Double): ISpectrumData
}