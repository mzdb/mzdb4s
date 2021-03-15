package com.github.mzdb4s.io.reader.param

import scala.collection.Seq
import com.github.mzdb4s.db.model.params._

trait IParamTreeParser {
  def parseParamTree(paramTreeAsStr: String): ParamTree

  def parseScanList(scanListAsStr: String): ScanList

  def parsePrecursors(precursorListAsStr: String): Seq[Precursor]

  def parseComponentList(componentListAsStr: String): ComponentList

  def parseFileContent(fileContentAsStr: String): FileContent
}
