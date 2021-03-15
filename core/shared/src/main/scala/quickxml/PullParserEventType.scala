package quickxml

object PullParserEventType extends Enumeration {
  val StartTag = Value(0)
  val StartEndTag = Value(1)
  val EndTag = Value(2)
  val Text = Value(3)
  val Eof = Value(4)
  val Unmatched = Value(5)

  def valueOf(id: Int): PullParserEventType.Value = {
    id match {
      case 0 => StartTag
      case 1 => StartEndTag
      case 2 => EndTag
      case 3 => Text
      case 4 => Eof
      case 5 => Unmatched
      case _ => throw new Exception(s"Invalid PullParserEventType value '$id'")
    }
  }
}