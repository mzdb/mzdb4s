package com.github.mzdb4s.io.reader.param

import com.github.mzdb4s.db.model.params._

trait IParamTreeParser {
  def parseParamTree(paramTreeAsStr: String): ParamTree

  def parseScanList(scanListAsStr: String): ScanList

  def parsePrecursor(precursorAsStr: String): Precursor

  def parseComponentList(componentListAsStr: String): ComponentList
}
