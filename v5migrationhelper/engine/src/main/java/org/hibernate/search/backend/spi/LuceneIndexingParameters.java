/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;

import org.hibernate.search.backend.configuration.impl.IndexWriterSetting;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Wrapper class around the Lucene indexing parameters defined in IndexWriterSetting.
 * <p>In previous versions of Hibernate Search you could set different values for batch
 * or transactional properties, these are now unified as different sets don't apply to
 * the internal design anymore.</p>
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters implements Serializable {

	private static final long serialVersionUID = 5424606407623591663L;
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	// value keyword
	public static final String EXPLICIT_DEFAULT_VALUE = "default";
	// property path keywords
	public static final String PROP_GROUP = "indexwriter";

	private final ParameterSet indexParameters;

	public LuceneIndexingParameters(Properties sourceProps) {
		Properties indexingParameters = new MaskedProperty( sourceProps, PROP_GROUP );
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

		final Map<IndexWriterSetting, Integer> parameters = new EnumMap<IndexWriterSetting, Integer>( IndexWriterSetting.class );

		public ParameterSet(Properties prop) {
			//don't iterate on property entries as we know all the keys:
			for ( IndexWriterSetting t : IndexWriterSetting.values() ) {
				String key = t.getKey();
				String value = prop.getProperty( key );
				if ( !( value == null || EXPLICIT_DEFAULT_VALUE.equalsIgnoreCase( value ) ) ) {
					if ( log.isDebugEnabled() ) {
						//TODO add DirectoryProvider name when available to log message
						log.debugf( "Set index writer parameter %s to value : %s", key, value );
					}
					parameters.put( t, t.parseVal( value ) );
				}
			}
		}

		/**
		 * Applies the parameters represented by this to a writer.
		 * Undefined parameters are not set, leaving the lucene default.
		 *
		 * @param writerConfig the IndexWriter configuration whereto the parameters will be applied.
		 */
		public void applyToWriter(IndexWriterConfig writerConfig) {
			for ( Map.Entry<IndexWriterSetting, Integer> entry : parameters.entrySet() ) {
				try {
					entry.getKey().applySetting( writerConfig, entry.getValue() );
				}
				catch (IllegalArgumentException e) {
					//TODO if DirectoryProvider had getDirectoryName() exceptions could tell better
					throw new SearchException(
							"Illegal IndexWriter setting "
									+ entry.getKey().getKey() + " " + e.getMessage(), e
					);
				}
			}
		}

		/**
		 * Creates a new LogByteSizeMergePolicy as configured by this property set.
		 * @return a new LogByteSizeMergePolicy instance.
		 */
		public LogByteSizeMergePolicy getNewMergePolicy() {
			LogByteSizeMergePolicy logByteSizeMergePolicy = new LogByteSizeMergePolicy();
			for ( Map.Entry<IndexWriterSetting, Integer> entry : parameters.entrySet() ) {
				try {
					entry.getKey().applySetting( logByteSizeMergePolicy, entry.getValue() );
				}
				catch (IllegalArgumentException e) {
					//TODO if DirectoryProvider had getDirectoryName() exceptions could tell better
					throw new SearchException(
							"Illegal IndexWriter setting "
									+ entry.getKey().getKey() + " " + e.getMessage(), e
					);
				}
			}
			return logByteSizeMergePolicy;
		}

		public Integer getCurrentValueFor(IndexWriterSetting ws) {
			return parameters.get( ws );
		}

		public void setCurrentValueFor(IndexWriterSetting ws, Integer newValue) {
			if ( newValue == null ) {
				parameters.remove( ws );
			}
			else {
				parameters.put( ws, newValue );
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ( ( parameters == null ) ? 0 : parameters.hashCode() );
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
			if ( parameters == null ) {
				if ( other.parameters != null ) {
					return false;
				}
			}
			else if ( !parameters.equals( other.parameters ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "ParameterSet" );
			sb.append( "{parameters=" ).append( parameters );
			sb.append( '}' );
			return sb.toString();
		}
	}

	public void applyToWriter(IndexWriterConfig writerConfig) {
		getIndexParameters().applyToWriter( writerConfig );
	}

}
