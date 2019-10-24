package com.github.mzdb4s.db.model

import com.github.mzdb4s.db.model.params._

// TODO: create serializers/deserializers (see IO project)
case class SpectrumMetaData(
  spectrumId: Long,
  paramTree: ParamTree,
  scanList: ScanList,
  precursorInformation: Option[Precursor]
  //productList: Option[String], // TODO: create model
)

case class SpectrumXmlMetaData(
  spectrumId: Long,
  paramTree: String,
  scanList: String,
  precursorList: Option[String],
  productList: Option[String]
)