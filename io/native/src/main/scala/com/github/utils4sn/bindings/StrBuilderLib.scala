package com.github.utils4sn.bindings

import scala.scalanative.unsafe._

@extern
@link("strbuilder") // TODO: link to other lib name?
object StrBuilderLib {

  type struct_str_builder_t = CStruct3[CString, CSize, CSize]
  type str_builder_t = struct_str_builder_t

  def str_builder_create(): Ptr[str_builder_t] = extern
  def str_builder_destroy(sb: Ptr[str_builder_t]): Unit = extern
  def str_builder_add_str(sb: Ptr[str_builder_t], str: CString, len: CSize): Unit = extern
  def str_builder_add_char(sb: Ptr[str_builder_t], c: CChar): Unit = extern
  def str_builder_add_int(sb: Ptr[str_builder_t], `val`: CInt): Unit = extern
  def str_builder_clear(sb: Ptr[str_builder_t]): Unit = extern
  def str_builder_truncate(sb: Ptr[str_builder_t], len: CSize): Unit = extern
  def str_builder_drop(sb: Ptr[str_builder_t], len: CSize): Unit = extern
  def str_builder_len(sb: Ptr[str_builder_t]): CSize = extern
  def str_builder_peek(sb: Ptr[str_builder_t]): CString = extern
  def str_builder_dump(sb: Ptr[str_builder_t], len: Ptr[CSize]): CString = extern

  def dtoa(v: CDouble, res: CString, decimalPlaces: CInt): Int = extern
  def ftoa(v: CFloat, res: CString, decimalPlaces: CInt): Int = extern
}

