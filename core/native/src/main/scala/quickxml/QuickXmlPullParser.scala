package quickxml

import com.github.sqlite4s.c.util.CUtils

import scala.scalanative.libc.stdlib
import scala.scalanative.runtime.ByteArray
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

object QuickXmlPullParser {

  def apply(xml: String): IQuickXmlPullParser = {
    new QuickXmlPullParser(xml)
  }

}

class QuickXmlPullParser private(xml: String) extends IQuickXmlPullParser {

  private val quickXml = QuickXmlLibrary

  private var xmlBytes: Array[Byte] = xml.getBytes()
  private var xmlPtr: CString = xmlBytes.asInstanceOf[ByteArray].at(0)
  private var parserPtr: Ptr[Byte] = quickXml.quickxml_create_pull_parser(xmlPtr)

  /*
  def parseNewDocument(xml: String): this.type = {
    if (parserPtr != null) {
      quickXml.quickxml_destroy_pull_parser(parserPtr)
    }

    //xmlPtr = toCString(xml)
    parserPtr = quickXml.quickxml_create_pull_parser(xmlPtr)

    this
  }*/

  def nextEvent(): PullParserEventType.Value = {
    assert(parserPtr != null, "parser is null")

    val rc = quickXml.quickxml_pull_parser_next_event(parserPtr)
    if (rc == -1) {
      throw new Exception("quickxml_pull_parser_next_event: unexpected error")
    }

    PullParserEventType.valueOf(rc)
  }

  def lastElementName(): Option[String] = {
    assert(parserPtr != null, "parser is null")

    val elemNamePtr: Ptr[Ptr[Byte]] = stackalloc[Ptr[Byte]](1L.toULong)
    val elemNameLenPtr: Ptr[CSize] = stackalloc[CSize](1L.toULong)

    val rc = quickXml.quickxml_pull_parser_elem_name(parserPtr,elemNamePtr, elemNameLenPtr)
    if (rc == 0) return None

    val str = _bytesPtr2String(elemNamePtr, elemNameLenPtr)

    Some(str)
  }

  def nextAttribute(): Option[(String,String)] = {
    assert(parserPtr != null, "parser is null")

    val keyPtr: Ptr[Ptr[Byte]] = stackalloc[Ptr[Byte]](1L.toULong)
    val keyLenPtr: Ptr[CSize] = stackalloc[CSize](1L.toULong)

    val valuePtr: Ptr[Ptr[Byte]] = stackalloc[Ptr[Byte]](1L.toULong)
    val valueLenPtr: Ptr[CSize] = stackalloc[CSize](1L.toULong)

    val rc = quickXml.quickxml_pull_parser_next_attribute(parserPtr, keyPtr, keyLenPtr, valuePtr, valueLenPtr)

    if (rc == 0) None
    else if (rc == -1) {
      throw new Exception("quickxml_pull_parser_next_attribute: unexpected error")
    } else {
      val key = _bytesPtr2String(keyPtr, keyLenPtr)
      val value = _bytesPtr2String(valuePtr, valueLenPtr)
      val attr = Some(key -> value)
      quickXml.quickxml_free_attribute(!keyPtr, !keyLenPtr, !valuePtr, !valueLenPtr)
      attr
    }

  }

  /*override def parseAttributes(): collection.Map[String, String] = {
    val attrsMap = new collection.mutable.HashMap[String, String]

    var hasAttribute = true
    while (hasAttribute) {
      val rc = quickXml.quickxml_pull_parser_next_attribute(parserPtr, keyPtr, keyLenPtr, valuePtr, valueLenPtr)
      if (rc == 0) hasAttribute = false
      else if (rc == -1) {
        throw new Exception("quickxml_pull_parser_next_attribute: unexpected error")
      } else {
        val key = _bytesPtr2String(keyPtr, keyLenPtr)
        val value = _bytesPtr2String(valuePtr, valueLenPtr)
        attrsMap.put(key, value)
      }
    }

    attrsMap
  }*/

  /*override def parseAttributes(attrsMap: collection.mutable.HashMap[String, String], clear: Boolean): collection.Map[String, String] = {
    if (clear) attrsMap.clear()

    var hasAttribute = true
    while (hasAttribute) {
      val rc = quickXml.quickxml_pull_parser_next_attribute(parserPtr, keyPtr, keyLenPtr, valuePtr, valueLenPtr)
      if (rc == 0) hasAttribute = false
      else if (rc == -1) {
        throw new Exception("quickxml_pull_parser_next_attribute: unexpected error")
      } else {
        val key = _bytesPtr2String(keyPtr, keyLenPtr)
        val value = _bytesPtr2String(valuePtr, valueLenPtr)
        attrsMap.put(key, value)
      }
    }

    attrsMap
  }*/

  @inline private def _bytesPtr2String(bytesPtr: Ptr[Ptr[Byte]], lengthPtr: Ptr[CSize]): String = {
    val bytes = CUtils.bytes2ByteArray(!bytesPtr, !lengthPtr)
    new String(bytes)
  }

  def dispose(): Unit = {
    if (parserPtr != null) {
      quickXml.quickxml_destroy_pull_parser(parserPtr)
      parserPtr = null
      xmlPtr = null
      xmlBytes = null
    }
  }

}
