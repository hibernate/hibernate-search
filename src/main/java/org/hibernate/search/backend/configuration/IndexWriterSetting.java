// $Id$
package org.hibernate.search.backend.configuration;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;

/**
 * Represents possible options to be applied to an
 * <code>org.apache.lucene.index.IndexWriter</code>
 * 
 * @author Sanne Grinovero
 */
public enum IndexWriterSetting implements Serializable {
	
	/**
	 * @see org.apache.lucene.index.IndexWriter#setMaxBufferedDeleteTerms(int)
	 */
	MAX_BUFFERED_DELETE_TERMS( "max_buffered_delete_terms" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxBufferedDeleteTerms( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setMaxBufferedDocs(int)
	 */
	MAX_BUFFERED_DOCS( "max_buffered_docs" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxBufferedDocs( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setMaxFieldLength(int)
	 */
	MAX_FIELD_LENGTH( "max_field_length" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxFieldLength( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setMaxMergeDocs(int)
	 */
	MAX_MERGE_DOCS( "max_merge_docs" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxMergeDocs( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setMergeFactor(int)
	 */
	MERGE_FACTOR( "merge_factor" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMergeFactor( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setRAMBufferSizeMB(double)
	 */
	RAM_BUFFER_SIZE( "ram_buffer_size" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setRAMBufferSizeMB( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setTermIndexInterval(int)
	 */
	TERM_INDEX_INTERVAL( "term_index_interval" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setTermIndexInterval( value );
		}
	} ,
	/**
	 * @see org.apache.lucene.index.IndexWriter#setUseCompoundFile(boolean)
	 */
	USE_COMPOUND_FILE( "use_compound_file" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setUseCompoundFile( intToBoolean( value ) );
		}
		@Override
		public Integer parseVal(String value) {
			return USE_COMPOUND_FILE.parseBoolean( value );
		}
	};
	
	private static final Integer TRUE = Integer.valueOf( 1 );
	private static final Integer FALSE = Integer.valueOf( 0 );
	
	private final String cfgKey;
	
	IndexWriterSetting(String configurationKey) {
		this.cfgKey = configurationKey;
	}
	
	/**
	 * @throws IllegalArgumentException when user selects an invalid value; should be wrapped.
	 */
	public abstract void applySetting(IndexWriter writer, int value);

	/**
	 * @return The key used in configuration files to select an option.
	 */
	public String getKey() {
		return cfgKey;
	}

	/**
	 * Specific parameters may override to provide additional keywords support.
	 * @param value the string value as in configuration file
	 * @return the integer value going to be set as parameter
	 * @throws SearchException for unrecognized values
	 */
	public Integer parseVal(String value) {
		return ConfigurationParseHelper.parseInt( value,
				"Invalid value for " + cfgKey + ": " + value );
	}
	
	private Integer parseBoolean(String value) {
		boolean v = ConfigurationParseHelper.parseBoolean( value,
				"Invalid value for " + cfgKey + ": " + value );
		return v ? TRUE : FALSE;
	}
	
	private static boolean intToBoolean(int value) {
		return value == TRUE.intValue();
	}
	
}
