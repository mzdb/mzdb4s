package com.github.mzdb4s.io.thermo.bindings

import scala.language.implicitConversions
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

@link("ThermoRawFileParser")
@extern
object MonoEmbeddinator {

  type enum_mono_embeddinator_error_type_t = CUnsignedInt
  object enum_mono_embeddinator_error_type_t {
    final val MONO_EMBEDDINATOR_OK: enum_mono_embeddinator_error_type_t = 0.toUInt
    final val MONO_EMBEDDINATOR_EXCEPTION_THROWN: enum_mono_embeddinator_error_type_t = 1.toUInt
    final val MONO_EMBEDDINATOR_ASSEMBLY_OPEN_FAILED: enum_mono_embeddinator_error_type_t = 2.toUInt
    final val MONO_EMBEDDINATOR_CLASS_LOOKUP_FAILED: enum_mono_embeddinator_error_type_t = 3.toUInt
    final val MONO_EMBEDDINATOR_METHOD_LOOKUP_FAILED: enum_mono_embeddinator_error_type_t = 4.toUInt
    final val MONO_EMBEDDINATOR_MONO_RUNTIME_MISSING_SYMBOLS: enum_mono_embeddinator_error_type_t = 5.toUInt
  }

  type __uint32_t = CUnsignedInt
  type uint32_t = __uint32_t
  type struct_MonoEmbedObject = CStruct2[Ptr[MonoClass], uint32_t]
  type MonoEmbedObject = struct_MonoEmbedObject
  type struct__MonoAssembly = CStruct0 // incomplete type
  type MonoAssembly = struct__MonoAssembly
  type struct__MonoImage = CStruct0 // incomplete type
  type MonoImage = struct__MonoImage
  type struct__MonoClass = CStruct0 // incomplete type
  type MonoClass = struct__MonoClass
  type struct__MonoDomain = CStruct0 // incomplete type
  type MonoDomain = struct__MonoDomain
  type struct__MonoObject = CStruct0 // incomplete type
  type MonoObject = struct__MonoObject
  type struct__MonoMethod = CStruct0 // incomplete type
  type MonoMethod = struct__MonoMethod
  type struct__MonoException = CStruct0 // incomplete type
  type MonoException = struct__MonoException
  type struct_mono_embeddinator_context_t = CStruct1[Ptr[MonoDomain]]
  type mono_embeddinator_context_t = struct_mono_embeddinator_context_t
  type mono_embeddinator_assembly_load_hook_t = CFuncPtr1[CString, Ptr[MonoAssembly]]
  type mono_embeddinator_error_type_t = enum_mono_embeddinator_error_type_t
  type struct_mono_embeddinator_error_t = CStruct3[mono_embeddinator_error_type_t, Ptr[MonoException], CString]
  type mono_embeddinator_error_t = struct_mono_embeddinator_error_t
  type mono_embeddinator_error_report_hook_t = CFuncPtr1[mono_embeddinator_error_t, Unit]
  def mono_embeddinator_init(ctx: Ptr[mono_embeddinator_context_t], domain: CString): CInt = extern
  def mono_embeddinator_destroy(ctx: Ptr[mono_embeddinator_context_t]): CInt = extern
  def mono_embeddinator_get_context(): Ptr[mono_embeddinator_context_t] = extern
  def mono_embeddinator_set_assembly_path(path: CString): Unit = extern
  def mono_embeddinator_set_runtime_assembly_path(path: CString): Unit = extern
  def mono_embeddinator_set_context(ctx: Ptr[mono_embeddinator_context_t]): Unit = extern
  def mono_embeddinator_load_assembly(ctx: Ptr[mono_embeddinator_context_t], assembly: CString): Ptr[MonoImage] = extern
  def mono_embeddinator_search_assembly(assembly: CString): CString = extern
  def mono_embeddinator_install_assembly_load_hook(hook: CFuncPtr1[CString, Ptr[MonoAssembly]]): CFuncPtr1[CString, Ptr[MonoAssembly]] = extern
  def mono_embeddinator_search_class(assembly: CString, _namespace: CString, name: CString): Ptr[MonoClass] = extern
  def mono_embeddinator_lookup_method(method_name: CString, klass: Ptr[MonoClass]): Ptr[MonoMethod] = extern
  def mono_embeddinator_throw_exception(exception: Ptr[MonoObject]): Unit = extern
  def mono_embeddinator_error_ptr_to_string(error: Ptr[mono_embeddinator_error_t]): CString = extern
  def mono_embeddinator_install_error_report_hook(hook: CFuncPtr1[mono_embeddinator_error_t, Unit]): Ptr[Byte] = extern
  def mono_embeddinator_create_object(instance: Ptr[MonoObject]): Ptr[Byte] = extern
  def mono_embeddinator_init_object(`object`: Ptr[MonoEmbedObject], instance: Ptr[MonoObject]): Unit = extern
  def mono_embeddinator_destroy_object(`object`: Ptr[MonoEmbedObject]): Unit = extern
  def mono_embeddinator_get_cultureinfo_invariantculture_object(): Ptr[MonoObject] = extern
  def mono_embeddinator_get_decimal_class(): Ptr[MonoClass] = extern
  def mono_embeddinator_get_datetime_class(): Ptr[MonoClass] = extern

  object implicits {
    implicit class struct_mono_embeddinator_context_t_ops(val p: Ptr[struct_mono_embeddinator_context_t]) extends AnyVal {
      def domain: Ptr[MonoDomain] = p._1
      def domain_=(value: Ptr[MonoDomain]): Unit = p._1 = value
    }

    implicit class struct_mono_embeddinator_error_t_ops(val p: Ptr[struct_mono_embeddinator_error_t]) extends AnyVal {
      def `type`: mono_embeddinator_error_type_t = p._1
      def `type_=`(value: mono_embeddinator_error_type_t): Unit = p._1 = value
      def exception: Ptr[MonoException] = p._2
      def exception_=(value: Ptr[MonoException]): Unit = { p._2 = value }
      def string: CString = p._3
      def string_=(value: CString): Unit = { p._3 = value }
    }

    implicit class struct_MonoEmbedObject_ops(val p: Ptr[struct_MonoEmbedObject]) extends AnyVal {
      def _class: Ptr[MonoClass] = p._1
      def _class_=(value: Ptr[MonoClass]): Unit = { p._1 = value }
      def _handle: uint32_t = p._2
      def _handle_=(value: uint32_t): Unit = { p._2 = value }
    }
  }

  object struct_mono_embeddinator_context_t {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_mono_embeddinator_context_t] = alloc[struct_mono_embeddinator_context_t]()
    def apply(domain: Ptr[MonoDomain])(implicit z: Zone): Ptr[struct_mono_embeddinator_context_t] = {
      val ptr = alloc[struct_mono_embeddinator_context_t]()
      ptr.domain = domain
      ptr
    }
  }

  object struct_mono_embeddinator_error_t {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_mono_embeddinator_error_t] = alloc[struct_mono_embeddinator_error_t]()
    def apply(`type`: mono_embeddinator_error_type_t, exception: Ptr[MonoException], string: CString)(implicit z: Zone): Ptr[struct_mono_embeddinator_error_t] = {
      val ptr = alloc[struct_mono_embeddinator_error_t]()
      ptr.`type` = `type`
      ptr.exception = exception
      ptr.string = string
      ptr
    }
  }

  object struct_MonoEmbedObject {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_MonoEmbedObject] = alloc[struct_MonoEmbedObject]()
    def apply(_class: Ptr[MonoClass], _handle: uint32_t)(implicit z: Zone): Ptr[struct_MonoEmbedObject] = {
      val ptr = alloc[struct_MonoEmbedObject]()
      ptr._class = _class
      ptr._handle = _handle
      ptr
    }
  }
}

