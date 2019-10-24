package com.github.utils4sn.bindings

import scala.scalanative.unsafe._

@link("yxml")
@extern
object YxmlLib {
  type enum_yxml_ret_t = CInt
  object enum_yxml_ret_t {
    final val YXML_EEOF: enum_yxml_ret_t = -5
    final val YXML_EREF: enum_yxml_ret_t = -4
    final val YXML_ECLOSE: enum_yxml_ret_t = -3
    final val YXML_ESTACK: enum_yxml_ret_t = -2
    final val YXML_ESYN: enum_yxml_ret_t = -1
    final val YXML_OK: enum_yxml_ret_t = 0
    final val YXML_ELEMSTART: enum_yxml_ret_t = 1
    final val YXML_CONTENT: enum_yxml_ret_t = 2
    final val YXML_ELEMEND: enum_yxml_ret_t = 3
    final val YXML_ATTRSTART: enum_yxml_ret_t = 4
    final val YXML_ATTRVAL: enum_yxml_ret_t = 5
    final val YXML_ATTREND: enum_yxml_ret_t = 6
    final val YXML_PISTART: enum_yxml_ret_t = 7
    final val YXML_PICONTENT: enum_yxml_ret_t = 8
    final val YXML_PIEND: enum_yxml_ret_t = 9
  }

  type __uint32_t = CUnsignedInt
  type __uint64_t = CUnsignedLong
  type uint32_t = __uint32_t
  type uint64_t = __uint64_t
  type yxml_ret_t = enum_yxml_ret_t
  type struct_yxml_t = CStruct16[CString, CArray[CChar, Nat._8], CString, CString, uint64_t, uint64_t, uint32_t, CInt, Ptr[CUnsignedChar], CSize, CSize, CUnsignedInt, CUnsignedInt, CInt, CUnsignedInt, Ptr[CUnsignedChar]]
  type yxml_t = struct_yxml_t

  def yxml_init(p0: Ptr[yxml_t], p1: Ptr[Byte], p2: CSize): Unit = extern
  def yxml_parse(p0: Ptr[yxml_t], p1: CInt): yxml_ret_t = extern
  def yxml_eof(p0: Ptr[yxml_t]): yxml_ret_t = extern
  def yxml_symlen(x: Ptr[yxml_t], s: CString): CSize = extern

  object implicits {
    implicit class struct_yxml_t_ops(val p: Ptr[struct_yxml_t]) extends AnyVal {
      def elem: CString = p._1
      //def elem_=(value: CString): Unit = !p._1 = value
      def data: Ptr[CArray[CChar, Nat._8]] = p.at2
      //def data_=(value: Ptr[CArray[CChar, Nat._8]]): Unit = !p._2 = !value
      def attr: CString = p._3
      //def attr_=(value: CString): Unit = !p._3 = value
      def pi: CString = p._4
      //def pi_=(value: CString): Unit = !p._4 = value
      def byte: uint64_t = p._5
      //def byte_=(value: uint64_t): Unit = !p._5 = value
      def total: uint64_t = p._6
      //def total_=(value: uint64_t): Unit = !p._6 = value
      def line: uint32_t = p._7
      //def line_=(value: uint32_t): Unit = !p._7 = value

      // PRIVATE struct fields (modified by DBO)
      /*def state: CInt = !p._8
      def state_=(value: CInt): Unit = !p._8 = value
      def stack: Ptr[CUnsignedChar] = !p._9
      def stack_=(value: Ptr[CUnsignedChar]): Unit = !p._9 = value
      def stacksize: CSize = !p._10
      def stacksize_=(value: CSize): Unit = !p._10 = value
      def stacklen: CSize = !p._11
      def stacklen_=(value: CSize): Unit = !p._11 = value
      def reflen: CUnsignedInt = !p._12
      def reflen_=(value: CUnsignedInt): Unit = !p._12 = value
      def quote: CUnsignedInt = !p._13
      def quote_=(value: CUnsignedInt): Unit = !p._13 = value
      def nextstate: CInt = !p._14
      def nextstate_=(value: CInt): Unit = !p._14 = value
      def ignore: CUnsignedInt = !p._15
      def ignore_=(value: CUnsignedInt): Unit = !p._15 = value
      def string: Ptr[CUnsignedChar] = !p._16
      def string_=(value: Ptr[CUnsignedChar]): Unit = !p._16 = value*/
    }
  }

  // We don't need a struct factory (modified by DBO)
  /*
  object struct_yxml_t {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_yxml_t] = alloc[struct_yxml_t]
    def apply(elem: CString, data: Ptr[CArray[CChar, Nat._8]], attr: CString, pi: CString, byte: uint64_t, total: uint64_t, line: uint32_t, state: CInt, stack: Ptr[CUnsignedChar], stacksize: CSize, stacklen: CSize, reflen: CUnsignedInt, quote: CUnsignedInt, nextstate: CInt, ignore: CUnsignedInt, string: Ptr[CUnsignedChar])(implicit z: Zone): Ptr[struct_yxml_t] = {
      val ptr = alloc[struct_yxml_t]
      ptr.elem = elem
      ptr.data = data
      ptr.attr = attr
      ptr.pi = pi
      ptr.byte = byte
      ptr.total = total
      ptr.line = line
      ptr.state = state
      ptr.stack = stack
      ptr.stacksize = stacksize
      ptr.stacklen = stacklen
      ptr.reflen = reflen
      ptr.quote = quote
      ptr.nextstate = nextstate
      ptr.ignore = ignore
      ptr.string = string
      ptr
    }
  }*/
}
