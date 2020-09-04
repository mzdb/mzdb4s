package com.github.mzdb4s.io.reader.param

import com.github.mzdb4s.db.model.params._

object NativeParamTreeParser extends IParamTreeParser {

  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    NativeParamTreeParserImpl.parseParamTree(paramTreeAsStr)
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    NativeParamTreeParserImpl.parseScanList(scanListAsStr)
  }

  def parsePrecursor(precursorAsStr: String): Precursor = {
    ParamTreeParserImpl.parsePrecursor(precursorAsStr)
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    ParamTreeParserImpl.parseComponentList(componentListAsStr)
  }
}
