package org.hibernate.search.backend.configuration;

import java.io.Serializable;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;

public enum IndexWriterSetting implements Serializable {
	
	MERGE_FACTOR( "merge_factor" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMergeFactor( value );
		}
	} ,
	MAX_MERGE_DOCS( "max_merge_docs" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxMergeDocs( value );
		}
	} ,
	MAX_BUFFERED_DOCS( "max_buffered_docs" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setMaxBufferedDocs( value );
		}
	} ,
	RAM_BUFFER_SIZE( "ram_buffer_size" ) {
		public void applySetting(IndexWriter writer, int value) {
			writer.setRAMBufferSizeMB( value );
		}
	};
	
	private final String cfgKey;
	
	IndexWriterSetting(String configurationKey){
		this.cfgKey = configurationKey;
	}
	
	/**
	 * @throws IllegalArgumentException when user selects an invalid value; should be wrapped.
	 */
	public abstract void applySetting(IndexWriter writer, int value);

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
		try {
			return Integer.valueOf( value );
		} catch (NumberFormatException ne) {
			throw new SearchException( "Invalid value for " + cfgKey + ": " + value );
		}
	}
	
}
