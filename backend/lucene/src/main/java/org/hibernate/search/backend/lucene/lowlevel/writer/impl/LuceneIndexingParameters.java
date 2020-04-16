/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;

/**
 * Wrapper class around the Lucene indexing parameters defined in IndexWriterSetting.
 * <p>In previous versions of Hibernate Search you could set different values for batch
 * or transactional properties, these are now unified as different sets don't apply to
 * the internal design anymore.</p>
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters {

	// property path keywords
	public static final String PROP_GROUP = "indexwriter";

	private final ParameterSet indexParameters;

	public LuceneIndexingParameters(ConfigurationPropertySource propertySource) {
		ConfigurationPropertySource indexingParameters = propertySource.withMask( PROP_GROUP );
		indexParameters = new ParameterSet( indexingParameters );
	}

	public ParameterSet getIndexParameters() {
		return indexParameters;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "LuceneIndexingParameters{" );
		sb.append( indexParameters );
		sb.append( '}' );
		return sb.toString();
	}

	public static class ParameterSet implements Serializable {

		private static final long serialVersionUID = -6121723702279869524L;

		private final List<IndexWriterSettingValue<?>> values;

		public ParameterSet(ConfigurationPropertySource prop) {
			values = IndexWriterSettings.extractAll( prop );
		}

		/**
		 * Applies the parameters represented by this to a writer.
		 * Undefined parameters are not set, leaving the lucene default.
		 *
		 * @param writerConfig the IndexWriter configuration whereto the parameters will be applied.
		 */
		public void applyToWriter(IndexWriterConfig writerConfig) {
			for ( IndexWriterSettingValue<?> value : values ) {
				value.applySetting( writerConfig );
			}
		}

		/**
		 * Creates a new LogByteSizeMergePolicy as configured by this property set.
		 * @return a new LogByteSizeMergePolicy instance.
		 */
		public LogByteSizeMergePolicy getNewMergePolicy() {
			LogByteSizeMergePolicy logByteSizeMergePolicy = new LogByteSizeMergePolicy();
			for ( IndexWriterSettingValue<?> value : values ) {
				value.applySetting( logByteSizeMergePolicy );
			}
			return logByteSizeMergePolicy;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + values.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			final ParameterSet other = (ParameterSet) obj;
			return values.equals( other.values );
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "ParameterSet" );
			sb.append( "{values=" ).append( values );
			sb.append( '}' );
			return sb.toString();
		}
	}

	public void applyToWriter(IndexWriterConfig writerConfig) {
		getIndexParameters().applyToWriter( writerConfig );
	}

}
