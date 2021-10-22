package com.github.mzdb4s.db.model.params.param

// TODO: move to parent package

object PsiMsCV extends Enumeration {
  protected final def Term(accession: String): Term = new Term(this.nextId, accession)
  class Term(i: Int, accession: String) extends Val(i: Int, accession: String) {
    def getAccession(): String = accession
  }

  // TODO: import full enumeration from PWIZ
  val ACQUISITION_PARAMETER = Term("MS:1001954")
  val CHARGE_STATE = Term("MS:1000041")
  val FILTER_STRING = Term("MS:1000512")
  val ISOLATION_WINDOW_TARGET_MZ = Term("MS:1000827")
  val ISOLATION_WINDOW_LOWER_OFFSET = Term("MS:1000828")
  val ISOLATION_WINDOW_UPPER_OFFSET = Term("MS:1000829")
  val MS_LEVEL = Term("MS:1000511")
  val SAMPLE_NAME = Term("MS:1000002")
  val SAMPLE_NUMBER = Term("MS:1000001")
  val SCAN_START_TIME = Term("MS:1000016")
  val SELECTED_ION_MZ = Term("MS:1000744")

  val CID = Term("MS:1000133")
  val ETD = Term("MS:1000598")
  val ETHCD = Term("MS:1002631")
  val HCD = Term("MS:1000422")
  val PSD = Term("MS:1000135")
}

/*object CVEntry extends Enumeration {
  val ACQUISITION_PARAMETER: CVEntry.Value = Value("MS:1001954")
  val FILTER_STRING: CVEntry.Value = Value("MS:1000512")
  val ISOLATION_WINDOW_TARGET_MZ: CVEntry.Value = Value("MS:1000827")
  val ISOLATION_WINDOW_LOWER_OFFSET: CVEntry.Value = Value("MS:1000828")
  val ISOLATION_WINDOW_UPPER_OFFSET: CVEntry.Value = Value("MS:1000829")
  val SELECTED_ION_MZ: CVEntry.Value = Value("MS:1000744")

  /**
    * Gets the accession.
    *
    * @return the accession
    */
  def getAccession(): String = this.toString
}*/