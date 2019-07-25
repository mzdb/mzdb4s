package com.github.mzdb4s.util.collection

import scala.collection.mutable.WrappedArray
import scala.reflect.ClassTag

object ResizedArray {

  final class ofInt(val array: Array[Int], val length: Int) extends WrappedArray[Int] with Serializable {
    require(length <= array.length,"length must be <= to array length")
    def elemTag: ClassTag[Int] = ClassTag.Int
    def apply(index: Int): Int = {
      if (index >= length) throw new IndexOutOfBoundsException(index.toString)
      array(index)
    }
    def update(index: Int, elem: Int) { array(index) = elem }
  }

  final class ofFloat(val array: Array[Float], val length: Int) extends WrappedArray[Float] with Serializable {
    require(length <= array.length,"length must be <= to array length")
    def elemTag: ClassTag[Float] = ClassTag.Float
    def apply(index: Int): Float = {
      if (index >= length) throw new IndexOutOfBoundsException(index.toString)
      array(index)
    }
    def update(index: Int, elem: Float) { array(index) = elem }
  }

  final class ofDouble(val array: Array[Double], val length: Int) extends WrappedArray[Double] with Serializable {
    require(length <= array.length,"length must be <= to array length")
    def elemTag: ClassTag[Double] = ClassTag.Double
    def apply(index: Int): Double = {
      if (index >= length) throw new IndexOutOfBoundsException(index.toString)
      array(index)
    }
    def update(index: Int, elem: Double) { array(index) = elem }
  }

}
