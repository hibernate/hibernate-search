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
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.IndexWriterSetting;
import org.hibernate.search.backend.configuration.MaskedProperty;
import org.hibernate.search.util.LoggerFactory;

import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_FIELD_LENGTH;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.USE_COMPOUND_FILE;

/**
 * Wrapper class around the Lucene indexing parameters defined in IndexWriterSetting.
 * <p>
 * There are two sets of these parameters. One is for regular indexing the other is for batch indexing
 * triggered by <code>FullTextSession.index(Object entity)</code>
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters implements Serializable {

	private static final long serialVersionUID = 5424606407623591663L;
	private static final Logger log = LoggerFactory.make();

	// value keyword
	public static final String EXPLICIT_DEFAULT_VALUE = "default";
	// property path keywords
	public static final String BATCH = "batch";
	public static final String TRANSACTION = "transaction";
	public static final String PROP_GROUP = "indexwriter";

	private final ParameterSet transactionIndexParameters;
	private final ParameterSet batchIndexParameters;

	public LuceneIndexingParameters(Properties sourceProps) {
		//prefer keys under "indexwriter" but fallback for backwards compatibility:
		Properties indexingParameters = new MaskedProperty( sourceProps, PROP_GROUP, sourceProps );
		//get keys for "transaction"
		Properties transactionProps = new MaskedProperty( indexingParameters, TRANSACTION );
		//get keys for "batch"
		Properties batchProps = new MaskedProperty( indexingParameters, BATCH );
		transactionIndexParameters = new ParameterSet( transactionProps, TRANSACTION );
		batchIndexParameters = new ParameterSet( batchProps, BATCH );
		doSanityChecks( transactionIndexParameters, batchIndexParameters );
	}

	private void doSanityChecks(ParameterSet transParams, ParameterSet batchParams) {
		if ( log.isWarnEnabled() ) {
			Integer maxFieldLengthTransaction = transParams.parameters.get( MAX_FIELD_LENGTH );
			Integer maxFieldLengthBatch = batchParams.parameters.get( MAX_FIELD_LENGTH );
			if ( notEquals( maxFieldLengthTransaction, maxFieldLengthBatch ) ) {
				log.warn(
						"The max_field_length value configured for transaction is "
								+ "different than the value configured for batch."
				);
			}
			Integer useCompoundTransaction = transParams.parameters.get( USE_COMPOUND_FILE );
			Integer useCompoundBatch = batchParams.parameters.get( USE_COMPOUND_FILE );
			if ( notEquals( useCompoundTransaction, useCompoundBatch ) ) {
				log.warn(
						"The IndexWriter setting \"use_compound_file\" for batch " +
								"mode can't be different from the transaction setting."
				);
			}
		}
	}

	private boolean notEquals(Integer a, Integer b) {
		if ( a == null && b == null ) {
			return false;
		}
		if ( a == null && b != null ) {
			return true;
		}
		if ( a != null && b == null ) {
			return true;
		}
		return a.intValue() != b.intValue();
	}

	public ParameterSet getTransactionIndexParameters() {
		return transactionIndexParameters;
	}

	public ParameterSet getBatchIndexParameters() {
		return batchIndexParameters;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "LuceneIndexingParameters" );
		sb.append( "{batchIndexParameters=" ).append( batchIndexParameters );
		sb.append( ", transactionIndexParameters=" ).append( transactionIndexParameters );
		sb.append( '}' );
		return sb.toString();
	}

	public static class ParameterSet implements Serializable {

		private static final long serialVersionUID = -6121723702279869524L;

		final Map<IndexWriterSetting, Integer> parameters = new EnumMap<IndexWriterSetting, Integer>( IndexWriterSetting.class );

		public ParameterSet(Properties prop, String paramName) {
			//don't iterate on property entries as we know all the keys:
			for ( IndexWriterSetting t : IndexWriterSetting.values() ) {
				String key = t.getKey();
				String value = prop.getProperty( key );
				if ( !( value == null || EXPLICIT_DEFAULT_VALUE.equalsIgnoreCase( value ) ) ) {
					if ( log.isDebugEnabled() ) {
						//TODO add DirectoryProvider name when available to log message
						log.debug( "Set index writer parameter " + paramName + "." + key + " to value : " + value );
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
				catch ( IllegalArgumentException e ) {
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
				catch ( IllegalArgumentException e ) {
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

	public void applyToWriter(IndexWriterConfig writerConfig, boolean batch) {
		if ( batch ) {
			getBatchIndexParameters().applyToWriter( writerConfig );
		}
		else {
			getTransactionIndexParameters().applyToWriter( writerConfig );
		}
	}

}
