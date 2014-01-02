/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.configuration.impl;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Represents possible options to be applied to an
 * {@code org.apache.lucene.index.IndexWriter}.
 *
 * @author Sanne Grinovero
 */
public enum IndexWriterSetting implements Serializable {

	/**
	 * @see org.apache.lucene.index.IndexWriterConfig#setMaxBufferedDeleteTerms(int)
	 */
	MAX_BUFFERED_DELETE_TERMS( "max_buffered_delete_terms" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setMaxBufferedDeleteTerms( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.IndexWriterConfig#setMaxBufferedDocs(int)
	 */
	MAX_BUFFERED_DOCS( "max_buffered_docs" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setMaxBufferedDocs( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setMaxMergeDocs(int)
	 */
	MAX_MERGE_DOCS( "max_merge_docs" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeDocs( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setMergeFactor(int)
	 */
	MERGE_FACTOR( "merge_factor" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMergeFactor( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setMinMergeMB(double)
	 */
	MERGE_MIN_SIZE( "merge_min_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMinMergeMB( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setMaxMergeMB(double)
	 */
	MERGE_MAX_SIZE( "merge_max_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeMB( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setMaxMergeMBForForcedMerge(double)
	 */
	MERGE_MAX_OPTIMIZE_SIZE( "merge_max_optimize_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeMBForForcedMerge( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setCalibrateSizeByDeletes(boolean)
	 */
	MERGE_CALIBRATE_BY_DELETES( "merge_calibrate_by_deletes" ) {
		@Override
		public Integer parseVal(String value) {
			return MERGE_CALIBRATE_BY_DELETES.parseBoolean( value );
		}
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			boolean calibrateByDeletes = intToBoolean( value );
			logByteSizeMergePolicy.setCalibrateSizeByDeletes( calibrateByDeletes );
		}
	},
	/**
	 * @see org.apache.lucene.index.IndexWriterConfig#setRAMBufferSizeMB(double)
	 */
	RAM_BUFFER_SIZE( "ram_buffer_size" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setRAMBufferSizeMB( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.IndexWriterConfig#setTermIndexInterval(int)
	 */
	TERM_INDEX_INTERVAL( "term_index_interval" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setTermIndexInterval( value );
		}
	},
	/**
	 * @see org.apache.lucene.index.LogByteSizeMergePolicy#setUseCompoundFile(boolean)
	 */
	USE_COMPOUND_FILE( "use_compound_file" ) {
		@Override
		public Integer parseVal(String value) {
			return USE_COMPOUND_FILE.parseBoolean( value );
		}

		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			boolean useCompoundFile = intToBoolean( value );
			logByteSizeMergePolicy.setUseCompoundFile( useCompoundFile );
		}
	};

	private static final Integer TRUE = 1;
	private static final Integer FALSE = 0;

	private final String cfgKey;

	IndexWriterSetting(String configurationKey) {
		this.cfgKey = configurationKey;
	}

	/**
	 * @throws IllegalArgumentException when user selects an invalid value; should be wrapped.
	 */
	public void applySetting(IndexWriterConfig writerConfig, int value) {
		// nothing to do unless overriden
	}
	/**
	 * @throws IllegalArgumentException when user selects an invalid value; should be wrapped.
	 */
	public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
		// nothing to do unless overriden
	}

	/**
	 * @return The key used in configuration files to select an option.
	 */
	public String getKey() {
		return cfgKey;
	}

	/**
	 * Specific parameters may override to provide additional keywords support.
	 *
	 * @param value the string value as in configuration file
	 * @return the integer value going to be set as parameter
	 * @throws SearchException for unrecognized values
	 */
	public Integer parseVal(String value) {
		return ConfigurationParseHelper.parseInt(
				value,
				"Invalid value for " + cfgKey + ": " + value
		);
	}

	private Integer parseBoolean(String value) {
		boolean v = ConfigurationParseHelper.parseBoolean(
				value,
				"Invalid value for " + cfgKey + ": " + value
		);
		return v ? TRUE : FALSE;
	}

	private static boolean intToBoolean(int value) {
		return value == TRUE;
	}
}
