package com.github.mzdb4s.msdata

import scala.scalanative.unsafe._

import com.github.sqlite4s.c.util.CIntEnum
import com.github.sqlite4s.c.util.CUIntEnum

object CTypes {

  /*type union_anonymous_1 = CArray[Byte, Nat._8]
    type union_anonymous_2 = CArray[Byte, Nat._8]*/

  /*type data_mode_enum = CInt
  type byte_order_enum = CUnsignedInt
  type peak_encoding_enum = CUnsignedInt
  type struct_libmzdb_data_encoding = CStruct5[CInt, data_mode_enum, peak_encoding_enum, CString, byte_order_enum]*/
  type struct_libmzdb_data_encoding = CStruct5[CInt, CDataMode.Value, CPeakEncoding.Value, CString, CByteOrder.Value]
  type libmzdb_data_encoding_t = struct_libmzdb_data_encoding

  // FXIME: in libmzdb first struct field is not a pointer
  type struct_libmzdb_spectrum_data = CStruct6[Ptr[libmzdb_data_encoding_t], CInt, Ptr[CDouble], Ptr[CFloat], Ptr[CFloat], Ptr[CFloat]]
  type libmzdb_spectrum_data_t = struct_libmzdb_spectrum_data

  type struct_libmzdb_data_point_32_32 = CStruct2[CFloat, CFloat]
  type libmzdb_data_point_32_32_t = struct_libmzdb_data_point_32_32

  type struct_libmzdb_data_point_64_32 = CStruct2[CDouble, CFloat]
  type libmzdb_data_point_64_32_t = struct_libmzdb_data_point_64_32

}

object CByteOrder extends CUIntEnum {

  final val BIG_ENDIAN = Value(0)
  final val LITTLE_ENDIAN = Value(1)

  def withName(valueName: String): Value = {
    valueName match {
      case "big_endian" => BIG_ENDIAN
      case "little_endian" => LITTLE_ENDIAN
    }
  }

  def stringOf(value: Value): String = {
    value match {
      case BIG_ENDIAN => "big_endian"
      case LITTLE_ENDIAN => "little_endian"
    }
  }
}

object CDataMode extends CIntEnum {
  final val PROFILE: Value = -1
  final val CENTROIDED: Value = 12
  final val FITTED: Value = 20

  def withName(valueName: String): Value = {
    valueName match {
      case "profile" => PROFILE
      case "centroided" => CENTROIDED
      case "fitted" => FITTED
    }
  }

  def stringOf(value: Value): String = {
    value match {
      case PROFILE => "profile"
      case CENTROIDED => "centroided"
      case FITTED => "fitted"
    }
  }
}

object CPeakEncoding extends CUIntEnum {
  final val LOW_RES_PEAK: Value = 8
  final val HIGH_RES_PEAK: Value = 12
  final val NO_LOSS_PEAK: Value = 16

  def withName(valueName: String): Value = {
    valueName match {
      case "LOW_RES_PEAK" => LOW_RES_PEAK
      case "HIGH_RES_PEAK" => HIGH_RES_PEAK
      case "NO_LOSS_PEAK" => NO_LOSS_PEAK
    }
  }
}

/*
class CDataEncoding(val p: Ptr[CTypes.struct_libmzdb_data_encoding]) extends AnyVal with IDataEncoding {
  import CTypes._

  def id: CInt = !p._1
  def id_=(value: CInt): Unit = !p._1 = value

  def mode: DataMode.Value = !p._2
  def mode_=(value: DataMode.Value): Unit = !p._2 = value

  def peak_encoding: PeakEncoding.Value = !p._3
  def peak_encoding_=(value: PeakEncoding.Value): Unit = !p._3 = value

  def compression: CString = !p._4
  def compression_=(value: CString): Unit = !p._4 = value

  def byte_order: ByteOrder.Value = !p._5
  def byte_order_=(value: ByteOrder.Value): Unit = !p._5 = value

  // Implementation of IDataEncoding interface
  def getId(): Int = id
  def getMode(): DataMode.Value = mode
  def getPeakEncoding(): PeakEncoding.Value = peak_encoding
  def getCompression(): String = CUtils.fromCString(compression)
  def getByteOrder(): ByteOrder.Value = byte_order

  def getPeakStructSize(): Int = {
    var peakBytesSize = peak_encoding.toInt
    if (mode == DataMode.FITTED) peakBytesSize += 8 // add 2 floats (left hwhm and right hwhm)
    peakBytesSize
  }

  def toDataEncoding(): DataEncoding = {
    DataEncoding(id, mode, peak_encoding, getCompression(), byte_order)
  }
}
 */


// TODO: implement this lower level data representation
/*
object struct_libmzdb_spectrum_data {

  import CTypes._

  def apply()(implicit z: Zone): Ptr[struct_libmzdb_spectrum_data] = alloc[struct_libmzdb_spectrum_data]
  def apply(
    data_encoding: Ptr[libmzdb_data_encoding_t],
    peak_count: CInt,
    mz_array: Ptr[CDouble], // TODO: Ptr[Byte]???
    intensity_array: Ptr[CFloat],  // TODO: Ptr[Byte]???
    lwhm_array: Ptr[CFloat],
    rwhm_array: Ptr[CFloat]
  )(implicit z: Zone): Ptr[struct_libmzdb_spectrum_data] = {
    var ptr = alloc[struct_libmzdb_spectrum_data]
    !ptr._1 = data_encoding
    !ptr._2 = peak_count
    !ptr._3 = mz_array
    !ptr._4 = intensity_array
    !ptr._5 = lwhm_array
    !ptr._6 = rwhm_array

    ptr
  }
}

/*
class CPeakCursor(val sd: CSpectrumData) extends AnyVal with IPeak {
  def getMz(): Double = sd.mz_array(0)
  def getIntensity(): Float = sd.intensity_array(0)
  def getLeftHwhm(): Option[Float] = sd.getLeftHwhmAt(0)
  def getRightHwhm(): Option[Float] = sd.getRightHwhmAt(0)
}*/

class CSpectrumData(val p: Ptr[CTypes.struct_libmzdb_spectrum_data]) extends AnyVal with ISpectrumData {
  def data_encoding: Ptr[CTypes.libmzdb_data_encoding_t] = !p._1
  def data_encoding_=(value: Ptr[CTypes.libmzdb_data_encoding_t]): Unit = !p._1 = value

  def peak_count: CInt = !p._2
  def peak_count_=(value: CInt): Unit = !p._2 = value

  def mz_array: Ptr[CDouble] = !p._3
  def mz_array_=(value: Ptr[CDouble]): Unit = !p._3 = value

  def intensity_array: Ptr[CFloat] = !p._4
  def intensity_array_=(value: Ptr[CFloat]): Unit = !p._4 = value

  def lwhm_array: Ptr[CFloat] = !p._5
  def lwhm_array_=(value: Ptr[CFloat]): Unit = !p._5 = value

  def rwhm_array: Ptr[CFloat] = !p._6
  def rwhm_array_=(value: Ptr[CFloat]): Unit = !p._6 = value

  def toSpectrumData(): SpectrumData = {
    SpectrumData(
      getDataEncoding().toDataEncoding(),
      CUtils.doublePtr2DoubleArray(mz_array, peak_count),
      CUtils.floatPtr2FloatArray(intensity_array, peak_count),
      CUtils.floatPtr2FloatArray(lwhm_array, peak_count),
      CUtils.floatPtr2FloatArray(rwhm_array, peak_count)
    )
  }

  // Implementation of ISpectrumData interface
  def getDataEncoding(): CDataEncoding = new CDataEncoding(data_encoding)

  def getMzAt(index: Int): Double = mz_array(index)

  def getIntensityAt(index: Int): Float = intensity_array(index)

  def getLeftHwhmAt(index: Int): Option[Float] = {
    val lwhms = lwhm_array
    if (lwhms != null) Some(lwhms(index)) else None
  }

  def getRightHwhmAt(index: Int): Option[Float] = {
    val rwhms = rwhm_array
    if (rwhms != null) Some(rwhms(index)) else None
  }

  def getPeaksCount(): Int = peak_count

  def forEachPeak(lcContext: ILcContext)(fn: IPeak => Unit): Unit = {

    val peakPtr = stackalloc[CPeak.struct_peak]

    var idx = 0
    while (idx < peak_count) {
      val peak = new CPeak(peakPtr)
      fn(peak)
      idx += 1
    }

    /*val cursorPtr = stackalloc[CTypes.struct_libmzdb_spectrum_data]
    val cursor = new CSpectrumData(cursorPtr)
    cursor.data_encoding = data_encoding
    cursor.peak_count = 1

    var idx = 0L
    while (idx < peak_count) {
      val mzPtr = mz_array + idx
      cursor.mz_array = mzPtr
      cursor.intensity_array = intensity_array + idx
      cursor.lwhm_array = lwhm_array + idx
      cursor.rwhm_array = rwhm_array + idx
      fn(new CPeakCursor(cursor))
      idx += 1
    }*/
  }

  /**
    * Gets the min mz.
    *
    * @return the min mz
    */
  def getMinMz(): Double = { // supposed and i hope it will always be true that mzList is sorted
    // do not do any verification
    if (peak_count == 0) return 0
    mz_array(0L)
  }

  /**
    * Gets the max mz.
    *
    * @return the max mz
    */
  def getMaxMz(): Double = {
    if (peak_count == 0) return 0
    mz_array(peak_count - 1)
  }

  /**
    * Checks if is empty.
    *
    * @return true, if is empty
    */
  def isEmpty(): Boolean = { // supposing intensityList and others have the same size
    peak_count == 0
  }
}

object CPeak {
  type struct_peak = CStruct4[CDouble, CFloat, CFloat, CFloat]

}

class CPeak(val p: Ptr[CPeak.struct_peak]) extends AnyVal with IPeak {
  def mz: CDouble = !p._1
  def mz_=(value: CDouble): Unit = !p._1 = value

  def intensity: CFloat = !p._2
  def intensity_=(value: CFloat): Unit = !p._2 = value

  def left_hwhm: CFloat = !p._3
  def left_hwhm_=(value: CFloat): Unit = !p._3 = value

  def right_hwhm: CFloat = !p._4
  def right_hwhm_=(value: CFloat): Unit = !p._4 = value

  def getMz(): Double = mz
  def getIntensity(): Float = intensity
  def getLeftHwhm(): Option[Float] = if (left_hwhm >= 0) Some(left_hwhm) else None
  def getRightHwhm(): Option[Float] = if (right_hwhm >= 0) Some(right_hwhm) else None
}
*/