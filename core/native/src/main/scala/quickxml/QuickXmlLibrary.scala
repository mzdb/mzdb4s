package quickxml

import scala.scalanative.unsafe._
//import scala.scalanative.unsigned._

@link("quickxml")
@extern
object QuickXmlLibrary {

  def quickxml_print_hello_world(): Unit = extern

  // Push parser API
  def quickxml_create_parser(): Ptr[Unit] = extern
  def quickxml_destroy_parser(parser: Ptr[Unit]): Unit = extern

  type quickxml_parser_cb = CFuncPtr2[Ptr[Byte], Int, Unit]
  def quickxml_register_cb(
    parser: Ptr[Unit],
    elemType: Int,
    cb: quickxml_parser_cb
  ): Unit = extern
  def quickxml_parse_xml(
    parser: Ptr[Unit],
    xml: CString
  ): Unit = extern

  // Pull parser API
  def quickxml_create_pull_parser(xml: CString): Ptr[Byte] = extern
  def quickxml_destroy_pull_parser(pullParser: Ptr[Byte]): Unit = extern
  def quickxml_pull_parser_next_event(pullParser: Ptr[Byte]): Int = extern // -1 or EventType
  def quickxml_pull_parser_elem_name(pullParser: Ptr[Byte], elemNamePtr: Ptr[Ptr[Byte]], elemLenPtr: Ptr[CSize]): Int = extern // 0 or 1
  def quickxml_pull_parser_next_attribute(
    pullParser: Ptr[Byte],
    keyPtr: Ptr[Ptr[Byte]], keyLenPtr: Ptr[CSize],
    valuePtr: Ptr[Ptr[Byte]], valueLenPtr: Ptr[CSize]
  ): Int = extern // -1, 0 or 1
  def quickxml_free_attribute(
    keyBytes: Ptr[Byte], keyLen: CSize,
    valueBytes: Ptr[Byte], valueLen: CSize
  ): Unit = extern
  //def quickxml_free_bytes(bytes: Ptr[Byte], nbytes: CSize): Unit = extern

}