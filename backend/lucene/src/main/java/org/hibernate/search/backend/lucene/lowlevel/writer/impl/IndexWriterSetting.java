/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.Serializable;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;

/**
 * Represents possible options to be applied to an
 * {@code org.apache.lucene.index.IndexWriter}.
 *
 * @author Sanne Grinovero
 */
public enum IndexWriterSetting implements Serializable {

	/**
	 * @see IndexWriterConfig#setMaxBufferedDocs(int)
	 */
	MAX_BUFFERED_DOCS( "max_buffered_docs" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setMaxBufferedDocs( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setMaxMergeDocs(int)
	 */
	MAX_MERGE_DOCS( "max_merge_docs" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeDocs( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setMergeFactor(int)
	 */
	MERGE_FACTOR( "merge_factor" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMergeFactor( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setMinMergeMB(double)
	 */
	MERGE_MIN_SIZE( "merge_min_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMinMergeMB( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setMaxMergeMB(double)
	 */
	MERGE_MAX_SIZE( "merge_max_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeMB( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setMaxMergeMBForForcedMerge(double)
	 */
	MERGE_MAX_OPTIMIZE_SIZE( "merge_max_optimize_size" ) {
		@Override
		public void applySetting(LogByteSizeMergePolicy logByteSizeMergePolicy, int value) {
			logByteSizeMergePolicy.setMaxMergeMBForForcedMerge( value );
		}
	},
	/**
	 * @see LogByteSizeMergePolicy#setCalibrateSizeByDeletes(boolean)
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
	 * @see IndexWriterConfig#setRAMBufferSizeMB(double)
	 */
	RAM_BUFFER_SIZE( "ram_buffer_size" ) {
		@Override
		public void applySetting(IndexWriterConfig writerConfig, int value) {
			writerConfig.setRAMBufferSizeMB( value );
		}
	},
	/**
	 * @see IndexWriterConfig#setInfoStream(org.apache.lucene.util.InfoStream)
	 */
	INFOSTREAM( "infostream" ) {
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
	 * @throws SearchException for unrecognized values
	 */
	public Integer parseVal(String value) {
		try {
			return ConvertUtils.convertInteger( value );
		}
		catch (RuntimeException e) {
			throw new SearchException( "Invalid value for " + cfgKey + ": " + value, e );
		}
	}

	private Integer parseBoolean(String value) {
		try {
			boolean v = ConvertUtils.convertBoolean( value );
			return v ? TRUE : FALSE;
		}
		catch (RuntimeException e) {
			throw new SearchException( "Invalid value for " + cfgKey + ": " + value, e );
		}
	}

	private static boolean intToBoolean(int value) {
		return value == TRUE;
	}
}
