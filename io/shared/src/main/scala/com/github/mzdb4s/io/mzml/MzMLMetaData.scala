package com.github.mzdb4s.io.mzml

import scala.beans.BeanProperty
import scala.collection.Seq

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params.FileContent
import com.github.mzdb4s.msdata.DataEncoding

case class MzMLMetaData(
  @BeanProperty fileContent: FileContent,
  @BeanProperty commonInstrumentParams: CommonInstrumentParams,
  @BeanProperty instrumentConfigurations: Seq[InstrumentConfiguration],
  @BeanProperty processingMethods: Seq[ProcessingMethod],
  @BeanProperty runs: Seq[Run],
  @BeanProperty samples: Seq[Sample],
  @BeanProperty softwareList: Seq[Software],
  @BeanProperty sourceFiles: Seq[SourceFile]
) {
  def toMzDbMetaData(mzDbHeader: MzDbHeader, dataEncodings: Seq[DataEncoding]): MzDbMetaData = {
    MzDbMetaData(
      mzDbHeader,
      dataEncodings,
      this.commonInstrumentParams,
      this.instrumentConfigurations,
      this.processingMethods,
      this.runs,
      this.samples,
      this.softwareList,
      this.sourceFiles
    )
  }
}
