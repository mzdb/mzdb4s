package com.github.mzdb4s.io.reader.param

import scala.collection.Seq
import com.github.mzdb4s.db.model.params._

object ParamTreeParser extends IParamTreeParser {

  def parseParamTree(paramTreeAsStr: String): ParamTree = {
    JVMParamTreeParserImpl.parseParamTree(paramTreeAsStr)
  }

  def parseScanList(scanListAsStr: String): ScanList = {
    JVMParamTreeParserImpl.parseScanList(scanListAsStr)
  }

  def parsePrecursors(precursorListAsStr: String): Seq[Precursor] = {
    JVMParamTreeParserImpl.parsePrecursors(precursorListAsStr)
  }

  def parseComponentList(componentListAsStr: String): ComponentList = {
    JVMParamTreeParserImpl.parseComponentList(componentListAsStr)
  }

  def parseFileContent(fileContentAsStr: String): FileContent = {
    JVMParamTreeParserImpl.parseFileContent(fileContentAsStr)
  }
}
