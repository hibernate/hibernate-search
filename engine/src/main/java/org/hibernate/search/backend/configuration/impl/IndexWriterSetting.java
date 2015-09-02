/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.configuration.impl;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.LoggerInfoStream;

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
	@Deprecated
	TERM_INDEX_INTERVAL( "term_index_interval" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			log.deprecatedConfigurationPropertyIsIgnored( "term_index_interval" );
		}
	},
	@Deprecated
	MAX_THREAD_STATES( "max_thread_states" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			log.deprecatedConfigurationPropertyIsIgnored( "max_thread_states" );
		}
	},
	/**
	 * @see org.apache.lucene.index.IndexWriterConfig#setInfoStream(org.apache.lucene.util.InfoStream)
	 */
	INFOSTREAM ( "infostream" ) {
		@Override
		public Integer parseVal(String value) {
			return INFOSTREAM.parseBoolean( value );
		}
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			boolean enableInfoStream = intToBoolean( value );
			if ( enableInfoStream ) {
				writerConfig.setInfoStream( new LoggerInfoStream() );
			}
		}
	};

	private static final Integer TRUE = 1;
	private static final Integer FALSE = 0;

	private static final Log log = LoggerFactory.make();

	private final String cfgKey;

	IndexWriterSetting(String configurationKey) {
		this.cfgKey = configurationKey;
	}

	/**
	 * @param writerConfig the {@link IndexWriterConfig}
	 * @param value the value for the configuration
	 * @throws IllegalArgumentException when user selects an invalid value; should be wrapped.
	 */
	public void applySetting(IndexWriterConfig writerConfig, int value) {
		// nothing to do unless overriden
	}
	/**
	 * @param logByteSizeMergePolicy the {@link LogByteSizeMergePolicy}
	 * @param value the value for the configuration
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
	 * @throws org.hibernate.search.exception.SearchException for unrecognized values
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
