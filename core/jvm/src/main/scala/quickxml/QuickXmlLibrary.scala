package quickxml

import jnr.ffi.{LibraryLoader, Pointer}
import jnr.ffi.annotations.{Delegate, In, Out}
import jnr.ffi.byref.PointerByReference
import jnr.ffi.types.size_t

object QuickXmlLibrary {

  trait OnEachStringCallback {
    @Delegate def onResult(
      @In bytesPtr: Pointer,
      @size_t length: Long
    ): Unit
  }

  trait QuickXmlInterface {
    def quickxml_print_hello_world(): Unit

    // Push parser API
    /*def quickxml_create_parser(): Pointer
    def quickxml_destroy_parser(parser: Pointer): Unit
    def quickxml_register_cb(
      parser: Pointer,
      elemType: Int,
      cb: OnEachStringCallback
    ): Unit
    def quickxml_parse_xml(
      parser: Pointer,
      xml: String
    ): Unit*/

    // Pull parser API
    def quickxml_create_pull_parser(@In xml: Pointer): Pointer
    def quickxml_destroy_pull_parser(@In pullParser: Pointer): Unit
    def quickxml_pull_parser_next_event(@In pullParser: Pointer): Int // -1 or EventType
    def quickxml_pull_parser_elem_name(@In pullParser: Pointer, @Out elemNamePtr: PointerByReference, @Out elemLenPtr: Pointer): Int // 0 or 1
    def quickxml_pull_parser_next_attribute(
      @In pullParser: Pointer,
      @Out keyPtr: PointerByReference, @Out keyLenPtr: Pointer,
      @Out valuePtr: PointerByReference, @Out valueLenPtr: Pointer
    ): Int // -1, 0 or 1
    def quickxml_free_attribute(
      keyBytes: Pointer, @size_t keyLen: Int,
      valueBytes: Pointer, @size_t valueLen: Int
    ): Unit
    //def quickxml_free_bytes(@In bytes: Pointer, @size_t nbytes: Int): Unit
  }

  private val rustLib: QuickXmlInterface = {
    import QuickXmlEnv._
    val libPath = getLibraryPath().getOrElse(extractLibrary().getAbsolutePath)
    LibraryLoader.create(classOf[QuickXmlInterface]).load(libPath)
  }
  private val runtime: jnr.ffi.Runtime = jnr.ffi.Runtime.getRuntime(rustLib)

  def getLibrary(): QuickXmlInterface = rustLib
  def getRuntime(): jnr.ffi.Runtime = runtime

  def printHelloWorld(): Unit = {
    assert(rustLib != null, "the library is not loaded")
    rustLib.quickxml_print_hello_world()
  }

}

