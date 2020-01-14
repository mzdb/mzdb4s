package com.github.mzdb4s.io.writer

object MzDbSchema {

  val schemaLines = Array(
    "CREATE TABLE data_processing (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL \n);\n",

    "CREATE TABLE scan_settings (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "param_tree,",
    "shared_param_tree_id INTEGER,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id) \n);\n",

    "CREATE TABLE data_encoding (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT, mode TEXT(10) NOT NULL,",
    "compression TEXT, byte_order TEXT(13) NOT NULL,",
    "mz_precision INTEGER NOT NULL,",
    "intensity_precision INTEGER NOT NULL,",
    "param_tree TEXT \n);\n",

    "CREATE TABLE software (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "version TEXT NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "shared_param_tree_id INTEGER,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id) \n);\n",

    "CREATE TABLE processing_method (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "number INTEGER NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "shared_param_tree_id INTEGER,",
    "data_processing_id INTEGER NOT NULL,",
    "software_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (data_processing_id) REFERENCES data_processing (id),",
    "FOREIGN KEY (software_id) REFERENCES software (id) \n);\n",

    "CREATE TABLE sample (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "param_tree TEXT,",
    "shared_param_tree_id INTEGER,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id) \n);\n",

    "CREATE TABLE source_file (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "location TEXT NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "shared_param_tree_id INTEGER,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id) \n);\n",

    "CREATE TABLE source_file_scan_settings_map (",
    "scan_settings_id INTEGER NOT NULL,",
    "source_file_id INTEGER NOT NULL,",
    "PRIMARY KEY (scan_settings_id, source_file_id) \n);\n",

    "CREATE TABLE cv (",
    "id TEXT(10) NOT NULL,",
    "full_name TEXT NOT NULL,",
    "version TEXT(10),",
    "uri TEXT NOT NULL,",
    "PRIMARY KEY (id) \n);\n",

    "CREATE TABLE param_tree_schema (",
    "name TEXT NOT NULL,",
    "type TEXT(10) NOT NULL,",
    "schema TEXT NOT NULL,PRIMARY KEY (name) \n);\n",

    "CREATE TABLE table_param_tree_schema (",
    "table_name TEXT NOT NULL,",
    "schema_name TEXT NOT NULL,",
    "PRIMARY KEY (table_name),",
    "FOREIGN KEY (schema_name) REFERENCES param_tree_schema (name) \n);\n",

    "CREATE TABLE shared_param_tree (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "data TEXT NOT NULL,",
    "schema_name TEXT NOT NULL,",
    "FOREIGN KEY (schema_name) REFERENCES param_tree_schema (name) \n);\n",

    "CREATE TABLE instrument_configuration (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "param_tree TEXT,",
    "component_list TEXT NOT NULL,",
    "shared_param_tree_id INTEGER,",
    "software_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (software_id) REFERENCES software (id) \n);\n",

    "CREATE TABLE mzdb (",
    "version TEXT(10) NOT NULL,",
    "creation_timestamp TEXT NOT NULL,",
    "file_content TEXT NOT NULL,",
    "contacts TEXT NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "PRIMARY KEY (version) \n);\n",

    "CREATE TABLE run (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "start_timestamp TEXT,",
    "param_tree TEXT,",
    "shared_param_tree_id INTEGER,",
    "sample_id INTEGER NOT NULL,",
    "default_instrument_config_id INTEGER NOT NULL,",
    "default_source_file_id INTEGER,",
    "default_scan_processing_id INTEGER NOT NULL,",
    "default_chrom_processing_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (sample_id) REFERENCES sample (id),",
    "FOREIGN KEY (default_instrument_config_id) REFERENCES instrument_configuration (id),",
    "FOREIGN KEY (default_source_file_id) REFERENCES source_file (id),",
    "FOREIGN KEY (default_scan_processing_id) REFERENCES data_processing (id),",
    "FOREIGN KEY (default_chrom_processing_id) REFERENCES data_processing (id) \n);\n",

    "CREATE TABLE chromatogram (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "activation_type TEXT(10),", // FIXME: was NOT NULL
    "data_points BLOB NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "precursor TEXT,",
    "product TEXT,",
    "shared_param_tree_id INTEGER,",
    "run_id INTEGER NOT NULL,",
    "data_processing_id INTEGER,",
    "data_encoding_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (run_id) REFERENCES run (id),",
    "FOREIGN KEY (data_processing_id) REFERENCES data_processing (id),",
    "FOREIGN KEY (data_encoding_id) REFERENCES data_encoding (id) \n);\n",

    "CREATE TABLE run_slice (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "ms_level INTEGER NOT NULL,",
    "number INTEGER NOT NULL,",
    "begin_mz REAL NOT NULL,",
    "end_mz REAL NOT NULL,",
    "param_tree TEXT,",
    "run_id INTEGER NOT NULL,",
    "FOREIGN KEY (run_id) REFERENCES run (id) \n);\n",

    "CREATE TEMPORARY TABLE tmp_spectrum (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "initial_id INTEGER NOT NULL,",
    "title TEXT NOT NULL,",
    "cycle INTEGER NOT NULL,",
    "time REAL NOT NULL,",
    "ms_level INTEGER NOT NULL,",
    "activation_type TEXT(10),", // FIXME: was NOT NULL
    "tic REAL NOT NULL,",
    "base_peak_mz REAL NOT NULL,",
    "base_peak_intensity REAL NOT NULL,",
    "main_precursor_mz REAL,",
    "main_precursor_charge INTEGER,",
    "data_points_count INTEGER NOT NULL,",
    "param_tree TEXT NOT NULL,",
    "scan_list TEXT,",
    "precursor_list TEXT,",
    "product_list TEXT,",
    "shared_param_tree_id INTEGER,",
    "instrument_configuration_id INTEGER,",
    "source_file_id INTEGER,",
    "run_id INTEGER NOT NULL,",
    "data_processing_id INTEGER,",
    "data_encoding_id INTEGER NOT NULL,",
    "bb_first_spectrum_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (instrument_configuration_id) REFERENCES instrument_configuration (id),",
    "FOREIGN KEY (source_file_id) REFERENCES source_file (id),",
    "FOREIGN KEY (run_id) REFERENCES run (id),",
    "FOREIGN KEY (data_processing_id) REFERENCES data_processing (id),",
    "FOREIGN KEY (data_encoding_id) REFERENCES data_encoding (id),",
    "FOREIGN KEY (bb_first_spectrum_id) REFERENCES tmp_spectrum (id) \n);\n",

    "CREATE TABLE bounding_box (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "data BLOB NOT NULL,",
    "run_slice_id INTEGER NOT NULL,",
    "first_spectrum_id INTEGER NOT NULL,",
    "last_spectrum_id INTEGER NOT NULL,",
    "FOREIGN KEY (run_slice_id) REFERENCES run_slice (id),",
    "FOREIGN KEY (first_spectrum_id) REFERENCES tmp_spectrum (id),",      // TODO: when do we reference spectrum instead of tmp_spectrum
    "FOREIGN KEY (last_spectrum_id) REFERENCES tmp_spectrum (id) \n);\n", // TODO: when do we reference spectrum instead of tmp_spectrum

    "CREATE TABLE cv_term (",
    "accession TEXT NOT NULL,",
    "name TEXT NOT NULL,",
    "unit_accession TEXT,",
    "cv_id TEXT(10) NOT NULL,",
    "PRIMARY KEY (accession),",
    "FOREIGN KEY (unit_accession) REFERENCES cv_unit (accession),",
    "FOREIGN KEY (cv_id) REFERENCES cv (id) \n);\n",

    "CREATE TABLE cv_unit (",
    "accession TEXT NOT NULL,",
    "name TEXT NOT NULL,",
    "cv_id TEXT(10) NOT NULL,",
    "PRIMARY KEY (accession),",
    "FOREIGN KEY (cv_id) REFERENCES cv (id) \n);\n",

    "CREATE TABLE user_term (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "name TEXT NOT NULL,",
    "type TEXT NOT NULL,",
    "unit_accession TEXT,",
    "FOREIGN KEY (unit_accession) REFERENCES cv_unit (accession) \n);\n",

    "CREATE TABLE target (",
    "id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "param_tree TEXT NOT NULL,",
    "shared_param_tree_id INTEGER,",
    "scan_settings_id INTEGER NOT NULL,",
    "FOREIGN KEY (shared_param_tree_id) REFERENCES shared_param_tree (id),",
    "FOREIGN KEY (scan_settings_id) REFERENCES scan_settings (id) \n);\n",

    "CREATE VIRTUAL TABLE bounding_box_rtree USING rtree(",
    "id INTEGER NOT NULL PRIMARY KEY,",
    "min_mz REAL NOT NULL,",
    "max_mz REAL NOT NULL,",
    "min_time REAL NOT NULL,",
    "max_time REAL NOT NULL \n);\n",

    "CREATE VIRTUAL TABLE bounding_box_msn_rtree USING rtree(",
    "id INTEGER NOT NULL PRIMARY KEY,",
    "min_ms_level REAL NOT NULL,",
    "max_ms_level REAL NOT NULL,",
    "min_parent_mz REAL NOT NULL,",
    "max_parent_mz REAL NOT NULL,",
    "min_mz REAL NOT NULL,",
    "max_mz REAL NOT NULL,",
    "min_time REAL NOT NULL,",
    "max_time REAL NOT NULL \n);\n"
  )


  def getDDLString(): String = schemaLines.mkString("\n")
}