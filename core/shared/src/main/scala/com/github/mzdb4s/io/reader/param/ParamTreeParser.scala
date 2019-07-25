package com.github.mzdb4s.io.reader.param

import com.github.mzdb4s.db.model.params._

object ParamTreeParser {

  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    ParamTreeParserImpl.parseParamTree(paramTreeAsStr)
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    ParamTreeParserImpl.parseScanList(scanListAsStr)
  }

  def parsePrecursor(precursorAsStr: String): Precursor = {
    ParamTreeParserImpl.parsePrecursor(precursorAsStr)
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    ParamTreeParserImpl.parseComponentList(componentListAsStr)
  }
}
