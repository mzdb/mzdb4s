package quickxml

trait IQuickXmlPullParser {

  def nextEvent(): PullParserEventType.Value
  def lastElementName(): Option[String]
  def nextAttribute(): Option[(String,String)]
  def dispose(): Unit

  def parseAttributes(attrsMap: collection.mutable.HashMap[String, String], clear: Boolean): collection.Map[String, String] = {
    if (clear) attrsMap.clear()

    var hasAttribute = true
    while (hasAttribute) {
      val attrOpt = this.nextAttribute()
      if (attrOpt.isEmpty) hasAttribute = false
      else {
        attrsMap += attrOpt.get
      }
    }

    attrsMap
  }

}
