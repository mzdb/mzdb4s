package com.github.mzdb4s.io.reader.param

import scala.collection.Seq
import com.github.mzdb4s.db.model.params._

object ParamTreeParser extends IParamTreeParser {
  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    NativeParamTreeParserImpl.parseParamTree(paramTreeAsStr)
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    NativeParamTreeParserImpl.parseScanList(scanListAsStr)
  }

  def parsePrecursors(precursorListAsStr: String): Seq[Precursor] = {
    NativeParamTreeParserImpl.parsePrecursors(precursorListAsStr)
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    NativeParamTreeParserImpl.parseComponentList(componentListAsStr)
  }

  def parseFileContent(fileContentAsStr: String): FileContent = {
    NativeParamTreeParserImpl.parseFileContent(fileContentAsStr)
  }
}
