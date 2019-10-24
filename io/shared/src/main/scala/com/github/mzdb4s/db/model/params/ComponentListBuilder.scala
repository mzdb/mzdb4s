package com.github.mzdb4s.db.model.params

class ComponentListBuilder() {

  private var order = 0
  private val components = new collection.mutable.ArrayBuffer[Component]()

  private def _injectParams[T <: Component](component: T, paramTree: AbstractParamTree): T = {
    component.setCVParams(paramTree.getCVParams())
    component.setUserParams(paramTree.getUserParams())
    component.setUserTexts(paramTree.getUserTexts())
    component
  }

  def addAnalyzerComponent(params: AbstractParamTree): this.type = {
    order += 1
    //components += AnalyzerComponent(order, params.cvParams, params.userParams, params.userTexts)

    val component = new AnalyzerComponent(order)
    components += _injectParams(component, params)

    this
  }

  def addDetectorComponent(params: ParamTree): this.type = {
    order += 1
    //components += DetectorComponent(order, params.cvParams, params.userParams, params.userTexts)

    val component = new DetectorComponent(order)
    components += _injectParams(component, params)

    this
  }

  def addSourceComponent(params: ParamTree): this.type = {
    order += 1
    //components += SourceComponent(order, params.cvParams, params.userParams, params.userTexts)

    val component = new SourceComponent(order)
    components += _injectParams(component, params)

    this
  }

  def toComponentList(): ComponentList = new ComponentList(components)
}
