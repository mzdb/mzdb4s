package quickxml

import jnr.ffi.Pointer
import jnr.ffi.byref.PointerByReference

//import java.nio.{ByteBuffer,CharBuffer}
import java.nio.charset.Charset

object QuickXmlPullParser {

  //private val charEncoder = Charset.defaultCharset().newEncoder()

  def apply(xml: String): IQuickXmlPullParser = {
    val parser = new QuickXmlPullParser()
    parser.parseNewDocument(xml)
  }

}

class QuickXmlPullParser() extends IQuickXmlPullParser {

  private val quickXml = QuickXmlLibrary.getLibrary()
  private val memoryManager = QuickXmlLibrary.getRuntime().getMemoryManager

  private val elemNamePtr = new PointerByReference()
  private val elemNameLenPtr = memoryManager.allocateDirect(4)

  private val keyPtr = new PointerByReference()
  private val keyLenPtr = memoryManager.allocateDirect(4)

  private val valuePtr = new PointerByReference()
  private val valueLenPtr = memoryManager.allocateDirect(4)

  private var parserPtr: Pointer = _
  private var xmlPtr: Pointer = _
  private var xmlPtrLen: Int = 0

  def parseNewDocument(xml: String): this.type = {
    if (parserPtr != null) {
      quickXml.quickxml_destroy_pull_parser(parserPtr)
    }

    val bytes = xml.getBytes()
    val nBytes = bytes.length

    if (xmlPtr == null || nBytes > xmlPtrLen) {
      xmlPtrLen = nBytes
      xmlPtr = memoryManager.allocateDirect(xmlPtrLen + 1)
    }
    _chars2pointer(bytes, xmlPtr)

    parserPtr = quickXml.quickxml_create_pull_parser(xmlPtr)

    this
  }

  // See: https://stackoverflow.com/questions/17737206/efficiently-convert-java-string-into-null-terminated-byte-representing-a-c-str
  @inline private def _chars2pointer(bytes: Array[Byte], bytesPtr: Pointer): Unit = {
    //val bytes = string.getBytes()
    /*val nBytes = bytes.length
    val bf = ByteBuffer.wrap(bytes, 0, nBytes)

    val len = string.length
    val b = new Array[Byte](len + 1)
    val bbuf = ByteBuffer.wrap(b)
    QuickXmlPullParser.charEncoder.encode(CharBuffer.wrap(string), bbuf, true)
    b(len) = 0

    Pointer.wrap(QuickXmlLibrary.getRuntime(),bf)*/

    bytesPtr.put(0,bytes,0,bytes.length)
    bytesPtr.putByte(bytes.length, 0) // nul terminator
  }

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

    val rc = quickXml.quickxml_pull_parser_elem_name(parserPtr,elemNamePtr, elemNameLenPtr)
    if (rc == 0) return None

    Some(_bytesPtr2String(elemNamePtr, elemNameLenPtr))
  }

  def nextAttribute(): Option[(String,String)] = {
    assert(parserPtr != null, "parser is null")

    val rc = quickXml.quickxml_pull_parser_next_attribute(parserPtr, keyPtr, keyLenPtr, valuePtr, valueLenPtr)
    if (rc == 0) None
    else if (rc == -1) {
      throw new Exception("quickxml_pull_parser_next_attribute: unexpected error")
    } else {
      val key = _bytesPtr2String(keyPtr, keyLenPtr)
      val value = _bytesPtr2String(valuePtr, valueLenPtr)
      val attr = Some(key -> value)

      quickXml.quickxml_free_attribute(
        keyPtr.getValue, keyLenPtr.getInt(0),
        valuePtr.getValue,valueLenPtr.getInt(0)
      )

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

  @inline private def _bytesPtr2String(bytesRef: PointerByReference, lengthRef: Pointer): String = {
    val bytesPtr = bytesRef.getValue
    val nBytes = lengthRef.getInt(0)
    val str = bytesPtr.getString(0, nBytes, Charset.defaultCharset())
    //quickXml.quickxml_free_bytes(bytesPtr, nBytes)
    str
  }

  def dispose(): Unit = {
    if (parserPtr != null) {
      quickXml.quickxml_destroy_pull_parser(parserPtr)
      parserPtr = null
      xmlPtr = null
    }
  }

  override def finalize(): Unit = this.dispose()
}