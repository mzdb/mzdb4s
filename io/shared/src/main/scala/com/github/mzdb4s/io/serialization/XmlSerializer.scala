package com.github.mzdb4s.io.serialization

import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._

object XmlSerializer {

  def serializeParamTree(paramTree: ParamTree): String = {
    if (paramTree == null) return null

    val strBuilder = new StringBuilder()
    strBuilder ++= "<params>\n"

    // --- Add CV params --- //
    val cvParams = paramTree.getCVParams()
    this._addCvParamsToStrBuilder(cvParams, strBuilder)

    // --- Add User params --- //
    val userParams = paramTree.getUserParams()
    if (userParams.nonEmpty) {
      strBuilder ++= "  <userParams>\n"
      _addXmlParamsToStrBuilder(userParams, strBuilder)
      strBuilder ++= "  </userParams>\n"
    }

    // --- Add User texts --- //
    val userTexts = paramTree.getUserTexts()
    if (userTexts.nonEmpty) {
      strBuilder ++= "  <userTexts>\n"
      _addXmlParamsToStrBuilder(userTexts, strBuilder)
      strBuilder ++= "  </userTexts>\n"
    }

    strBuilder ++= "</params>"

    strBuilder.toString()
  }

  def serializeComponentList(componentList: ComponentList): String = {
    if (componentList == null) return null

    val strBuilder = new StringBuilder()
    strBuilder ++= s"""<componentList count="${componentList.count}">"""

    componentList.components.sortBy(_.order).foreach { component =>
      val componentOrder = component.order
      val cvParams = component.getCVParams()

      // TODO: add userParams if any
      def serializeComponent(componentName: String): Unit = {
        strBuilder ++= s"""  <$componentName order="$componentOrder">\n"""
        this._addCvParamsToStrBuilder(cvParams, strBuilder)
        strBuilder ++= s"""  </$componentName>\n"""
      }

      component match {
        case ac: AnalyzerComponent => serializeComponent("analyzer")
        case dc: DetectorComponent => serializeComponent("detector")
        case sc: SourceComponent => serializeComponent("source")
        case _ => throw new IllegalArgumentException("invalid component type")
      }
    }

    strBuilder ++= "</componentList>"

    strBuilder.toString()

    /*"""<componentList count="3">
      |  <source order="1">
      |    <cvParam cvRef="MS" accession="MS:1000398" name="nanoelectrospray" value=""/>
      |    <cvParam cvRef="MS" accession="MS:1000485" name="nanospray inlet" value=""/>
      |  </source>
      |  <analyzer order="2">
      |    <cvParam cvRef="MS" accession="MS:1000484" name="orbitrap" value=""/>
      |  </analyzer>
      |  <detector order="3">
      |    <cvParam cvRef="MS" accession="MS:1000624" name="inductive detector" value=""/>
      |  </detector>
      |</componentList>
      |""".stripMargin*/
  }

  private def _addCvParamsToStrBuilder(cvParams: Seq[CVParam], strBuilder: StringBuilder): StringBuilder = {
    if (cvParams.nonEmpty) {
      strBuilder ++= "  <cvParams>\n"
      _addXmlParamsToStrBuilder(cvParams, strBuilder)
      strBuilder ++= "  </cvParams>\n"
    }

    strBuilder
  }

  @inline private def _addXmlParamsToStrBuilder(xmlParams: Seq[IXmlParam], strBuilder: StringBuilder): StringBuilder = {
    xmlParams.foreach { xmlParam =>
      strBuilder ++= s"    ${xmlParam.toXml()}\n"
    }
    strBuilder
  }


}
