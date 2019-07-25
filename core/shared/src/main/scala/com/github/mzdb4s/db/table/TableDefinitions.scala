package com.github.mzdb4s.db.table

object BoundingBoxMsnRtreeTable extends AbstractTableDefinition {
  val ID = Column("id")
  val MIN_MS_LEVEL = Column("min_ms_level")
  val MAX_MS_LEVEL = Column("max_ms_level")
  val MIN_PARENT_MZ = Column("min_parent_mz")
  val MAX_PARENT_MZ = Column("max_parent_mz")
  val MIN_MZ = Column("min_mz")
  val MAX_MZ = Column("max_mz")
  val MIN_TIME = Column("min_time")
  val MAX_TIME = Column("max_time")

  val tableName = "bounding_box_msn_rtree"
}

object BoundingBoxRtreeTable extends AbstractTableDefinition {
  val ID = Column("id")
  val MIN_MZ = Column("min_mz")
  val MAX_MZ = Column("max_mz")
  val MIN_TIME = Column("min_time")
  val MAX_TIME = Column("max_time")

  var tableName = "bounding_box_rtree"
}

object BoundingBoxTable extends AbstractTableDefinition {
  val ID = Column("id")
  val DATA = Column("data")
  val RUN_SLICE_ID = Column("run_slice_id")
  val FIRST_SPECTRUM_ID = Column("first_spectrum_id")
  val LAST_SPECTRUM_ID = Column("last_spectrum_id")

  var tableName = "bounding_box"
}

object ChromatogramTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val ACTIVATION_TYPE = Column("activation_type")
  val DATA_POINTS = Column("data_points")
  val PARAM_TREE = Column("param_tree")
  val PRECURSOR = Column("precursor")
  val PRODUCT = Column("product")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")
  val RUN_ID = Column("run_id")
  val DATA_PROCESSING_ID = Column("data_processing_id")
  val DATA_ENCODING_ID = Column("data_encoding_id")

  var tableName = "chromatogram"
}

object ControlledVocabularyTable extends AbstractTableDefinition {
  val ID = Column("id")
  val FULL_NAME = Column("full_name")
  val VERSION = Column("version")
  val URI = Column("uri")

  var tableName = "controlled_vocabulary"
}

object CvTable extends AbstractTableDefinition {
  val ID = Column("id")
  val FULL_NAME = Column("full_name")
  val VERSION = Column("version")
  val URI = Column("uri")

  var tableName = "cv"
}

object DataEncodingTable extends AbstractTableDefinition {
  val ID = Column("id")
  val MODE = Column("mode")
  val COMPRESSION = Column("compression")
  val BYTE_ORDER = Column("byte_order")
  val MZ_PRECISION = Column("mz_precision")
  val INTENSITY_PRECISION = Column("intensity_precision")
  val PARAM_TREE = Column("param_tree")

  var tableName = "data_encoding"
}

object DataProcessingTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")

  var tableName = "data_processing"
}

object InstrumentConfigurationTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val PARAM_TREE = Column("param_tree")
  val COMPONENT_LIST = Column("component_list")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")
  val SOFTWARE_ID = Column("software_id")

  var tableName = "instrument_configuration"
}

object MzdbTable extends AbstractTableDefinition {
  val VERSION = Column("version")
  val CREATION_TIMESTAMP = Column("creation_timestamp")
  val FILE_CONTENT = Column("file_content")
  val CONTACTS = Column("contacts")
  val PARAM_TREE = Column("param_tree")

  var tableName = "mzdb"
}

object ParamTreeSchemaTable extends AbstractTableDefinition {
  val NAME = Column("name")
  val TYPE = Column("type")
  val SCHEMA = Column("schema")

  var tableName = "param_tree_schema"
}

object ProcessingMethodTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NUMBER = Column("number")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")
  val DATA_PROCESSING_ID = Column("data_processing_id")
  val SOFTWARE_ID = Column("software_id")

  var tableName = "processing_method"
}

object RunSliceTable extends AbstractTableDefinition {
  val ID = Column("id")
  val MS_LEVEL = Column("ms_level")
  val NUMBER = Column("number")
  val BEGIN_MZ = Column("begin_mz")
  val END_MZ = Column("end_mz")
  val PARAM_TREE = Column("param_tree")
  val RUN_ID = Column("run_id")

  var tableName = "run_slice"
}

object RunTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val START_TIMESTAMP = Column("start_timestamp")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")
  val SAMPLE_ID = Column("sample_id")
  val DEFAULT_INSTRUMENT_CONFIG_ID = Column("default_instrument_config_id")
  val DEFAULT_SOURCE_FILE_ID = Column("default_source_file_id")
  val DEFAULT_SCAN_PROCESSING_ID = Column("default_scan_processing_id")
  val DEFAULT_CHROM_PROCESSING_ID = Column("default_chrom_processing_id")

  var tableName = "run"
}

object SampleTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")

  var tableName = "sample"
}

object ScanSettingsTable extends AbstractTableDefinition {
  val ID = Column("id")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")

  var tableName = "scan_settings"
}

object SharedParamTreeTable extends AbstractTableDefinition {
  val ID = Column("id")
  val DATA = Column("data")
  val SCHEMA_NAME = Column("schema_name")

  var tableName = "shared_param_tree"
}

object SoftwareTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val VERSION = Column("version")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE = Column("shared_param_tree")

  var tableName = "software"
}

object SourceFileScanSettingsMapTable extends AbstractTableDefinition {
  val SCAN_SETTINGS_ID = Column("scan_settings_id")
  val SOURCE_FILE_ID = Column("source_file_id")

  var tableName = "source_file_scan_settings_map"
}

object SourceFileTable extends AbstractTableDefinition {
  val ID = Column("id")
  val NAME = Column("name")
  val LOCATION = Column("location")
  val PARAM_TREE = Column("param_tree")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")

  var tableName = "source_file"
}


object SpectrumTable extends AbstractTableDefinition {
  val ID = Column("id")
  val INITIAL_ID = Column("initial_id")
  val TITLE = Column("title")
  val CYCLE = Column("cycle")
  val TIME = Column("time")
  val MS_LEVEL = Column("ms_level")
  val ACTIVATION_TYPE = Column("activation_type")
  val TIC = Column("tic")
  val BASE_PEAK_MZ = Column("base_peak_mz")
  val BASE_PEAK_INTENSITY = Column("base_peak_intensity")
  val MAIN_PRECURSOR_MZ = Column("main_precursor_mz")
  val MAIN_PRECURSOR_CHARGE = Column("main_precursor_charge")
  val DATA_POINTS_COUNT = Column("data_points_count")
  val PARAM_TREE = Column("param_tree")
  val SCAN_LIST = Column("scan_list")
  val PRECURSOR_LIST = Column("precursor_list")
  val PRODUCT_LIST = Column("product_list")
  val SHARED_PARAM_TREE_ID = Column("shared_param_tree_id")
  val INSTRUMENT_CONFIGURATION_ID = Column("instrument_configuration_id")
  val SOURCE_FILE_ID = Column("source_file_id")
  val RUN_ID = Column("run_id")
  val DATA_PROCESSING_ID = Column("data_processing_id")
  val DATA_ENCODING_ID = Column("data_encoding_id")
  val BB_FIRST_SPECTRUM_ID = Column("bb_first_spectrum_id")

  var tableName = "spectrum"
}

object TableParamTreeSchemaTable extends AbstractTableDefinition {
  val TABLE_NAME = Column("table_name")
  val SCHEMA_NAME = Column("schema_name")

  var tableName = "table_param_tree_schema"
}

/*
object BoundingBoxMsnRtreeTable extends AbstractTableDefinition {
  val ID, MIN_MS_LEVEL, MAX_MS_LEVEL, MIN_PARENT_MZ, MAX_PARENT_MZ, MIN_MZ, MAX_MZ, MIN_TIME, MAX_TIME = Column
  val tableName = "bounding_box_msn_rtree"
}

object BoundingBoxRtreeTable extends AbstractTableDefinition {
  val ID, MIN_MZ, MAX_MZ, MIN_TIME, MAX_TIME = Column
  var tableName = "bounding_box_rtree"
}

object BoundingBoxTable extends AbstractTableDefinition {
  val ID, DATA, RUN_SLICE_ID, FIRST_SPECTRUM_ID, LAST_SPECTRUM_ID = Column
  var tableName = "bounding_box"
}

object ChromatogramTable extends AbstractTableDefinition {
  val ID, NAME, ACTIVATION_TYPE, DATA_POINTS, PARAM_TREE, PRECURSOR, PRODUCT, SHARED_PARAM_TREE_ID, RUN_ID, DATA_PROCESSING_ID, DATA_ENCODING_ID = Column
  var tableName = "chromatogram"
}

object ControlledVocabularyTable extends AbstractTableDefinition {
  val ID, FULL_NAME, VERSION, URI = Column
  var tableName = "controlled_vocabulary"
}

object CvTable extends AbstractTableDefinition {
  val ID, FULL_NAME, VERSION, URI = Column
  var tableName = "cv"
}

object DataEncodingTable extends AbstractTableDefinition {
  val ID, MODE, COMPRESSION, BYTE_ORDER, MZ_PRECISION, INTENSITY_PRECISION, PARAM_TREE = Column
  var tableName = "data_encoding"
}

object DataProcessingTable extends AbstractTableDefinition {
  val ID, NAME = Column
  var tableName = "data_processing"
}

object InstrumentConfigurationTable extends AbstractTableDefinition {
  val ID, NAME, PARAM_TREE, COMPONENT_LIST, SHARED_PARAM_TREE_ID, SOFTWARE_ID = Column
  var tableName = "instrument_configuration"
}

object MzdbTable extends AbstractTableDefinition {
  val VERSION, CREATION_TIMESTAMP, FILE_CONTENT, CONTACTS, PARAM_TREE = Column
  var tableName = "mzdb"
}

object ParamTreeSchemaTable extends AbstractTableDefinition {
  val NAME, TYPE, SCHEMA = Column
  var tableName = "param_tree_schema"
}

object ProcessingMethodTable extends AbstractTableDefinition {
  val ID, NUMBER, PARAM_TREE, SHARED_PARAM_TREE_ID, DATA_PROCESSING_ID, SOFTWARE_ID = Column
  var tableName = "processing_method"
}

object RunSliceTable extends AbstractTableDefinition {
  val ID, MS_LEVEL, NUMBER, BEGIN_MZ, END_MZ, PARAM_TREE, RUN_ID = Column
  var tableName = "run_slice"
}

object RunTable extends AbstractTableDefinition {
  val ID, NAME, START_TIMESTAMP, PARAM_TREE, SHARED_PARAM_TREE_ID, SAMPLE_ID, DEFAULT_INSTRUMENT_CONFIG_ID, DEFAULT_SOURCE_FILE_ID, DEFAULT_SCAN_PROCESSING_ID, DEFAULT_CHROM_PROCESSING_ID = Column
  var tableName = "run"
}

object SampleTable extends AbstractTableDefinition {
  val ID, NAME, PARAM_TREE, SHARED_PARAM_TREE_ID = Column
  var tableName = "sample"
}

object ScanSettingsTable extends AbstractTableDefinition {
  val ID, PARAM_TREE, SHARED_PARAM_TREE_ID = Column
  var tableName = "scan_settings"
}

object SharedParamTreeTable extends AbstractTableDefinition {
  val ID, DATA, SCHEMA_NAME = Column
  var tableName = "shared_param_tree"
}

object SoftwareTable extends AbstractTableDefinition {
  val ID, NAME, VERSION, PARAM_TREE, SHARED_PARAM_TREE = Column
  var tableName = "software"
}

object SourceFileScanSettingsMapTable extends AbstractTableDefinition {
  val SCAN_SETTINGS_ID, SOURCE_FILE_ID = Column
  var tableName = "source_file_scan_settings_map"
}

object SourceFileTable extends AbstractTableDefinition {
  val ID, NAME, LOCATION, PARAM_TREE, SHARED_PARAM_TREE_ID = Column
  var tableName = "source_file"
}


object SpectrumTable extends AbstractTableDefinition {
  val ID, INITIAL_ID, TITLE, CYCLE, TIME, MS_LEVEL, ACTIVATION_TYPE, TIC, BASE_PEAK_MZ, BASE_PEAK_INTENSITY, MAIN_PRECURSOR_MZ, MAIN_PRECURSOR_CHARGE, DATA_POINTS_COUNT, PARAM_TREE, SCAN_LIST, PRECURSOR_LIST, PRODUCT_LIST, SHARED_PARAM_TREE_ID, INSTRUMENT_CONFIGURATION_ID, SOURCE_FILE_ID, RUN_ID, DATA_PROCESSING_ID, DATA_ENCODING_ID, BB_FIRST_SPECTRUM_ID = Column
  var tableName = "spectrum"
}

object TableParamTreeSchemaTable extends AbstractTableDefinition {
  val TABLE_NAME, SCHEMA_NAME = Column
  var tableName = "table_param_tree_schema"
}
*/