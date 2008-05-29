//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.IndexWriterSetting;
import org.hibernate.search.backend.configuration.MaskedProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_FIELD_LENGTH;

/**
 * Wrapper class around the Lucene indexing parameters defined in IndexWriterSetting.
 * <p>
 * There are two sets of these parameters. One is for regular indexing the other is for batch indexing
 * triggered by <code>FullTextSessoin.index(Object entity)</code>
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters implements Serializable {

	private static final long serialVersionUID = 5424606407623591663L;
	
	private final Logger log = LoggerFactory.getLogger( LuceneIndexingParameters.class );
	
	// value keyword
	public static final String EXPLICIT_DEFAULT_VALUE = "default";
	// property path keywords
	public static final String BATCH = "batch";
	public static final String TRANSACTION = "transaction";
	public static final String PROP_GROUP = "indexwriter";
	
	private final ParameterSet transactionIndexParameters;
	private final ParameterSet batchIndexParameters;
	
	public LuceneIndexingParameters( Properties sourceProps ) {
		//prefer keys under "indexwriter" but fallback for backwards compatibility:
		Properties indexingParameters = new MaskedProperty( sourceProps, PROP_GROUP, sourceProps );
		//get keys for "transaction"
		Properties transactionProps = new MaskedProperty( indexingParameters, TRANSACTION );
		//get keys for "batch" (defaulting to transaction)
		Properties batchProps = new MaskedProperty( indexingParameters, BATCH, transactionProps ); //TODO to close HSEARCH-201 just remove 3° parameter
		transactionIndexParameters = new ParameterSet( transactionProps, TRANSACTION );
		batchIndexParameters = new ParameterSet( batchProps, BATCH );
		doSanityChecks( transactionIndexParameters, batchIndexParameters );
	}

	private void doSanityChecks(ParameterSet transParams, ParameterSet batchParams) {
		if ( log.isWarnEnabled() ) {
			Integer maxFieldLengthTransaction = transParams.parameters.get( MAX_FIELD_LENGTH );
			Integer maxFieldLengthBatch = transParams.parameters.get( MAX_FIELD_LENGTH );
			if ( notEquals( maxFieldLengthTransaction, maxFieldLengthBatch ) ){
				log.warn( "The max_field_length value configured for transaction is different than the value configured for batch." );
			}
		}
	}

	private boolean notEquals(Integer a, Integer b) {
		if ( a==null && b==null ) return false;
		if ( a==null && b!=null ) return true;
		if ( a!=null && b==null ) return true;
		return a.intValue() != b.intValue();
	}

	public ParameterSet getTransactionIndexParameters() {
		return transactionIndexParameters;
	}

	public ParameterSet getBatchIndexParameters() {
		return batchIndexParameters;
	}

	public class ParameterSet implements Serializable {
		
		private static final long serialVersionUID = -6121723702279869524L;
		
		final Map<IndexWriterSetting, Integer> parameters = new HashMap<IndexWriterSetting, Integer>();
		
		public ParameterSet(Properties prop, String paramName) {
			//don't iterate on property entries as we know all the keys:
			for ( IndexWriterSetting t : IndexWriterSetting.values() ) {
				String key = t.getKey();
				String value = prop.getProperty( key );
				if ( ! ( value==null || EXPLICIT_DEFAULT_VALUE.equalsIgnoreCase( value ) ) ) {
					if ( log.isDebugEnabled() ) {
						//TODO add DirectoryProvider name when available to log message
						log.debug( "Set indexwriter parameter " + paramName +"." + key + " to value : "+ value );
					}
					parameters.put( t, t.parseVal( value ) );
				}
			}
		}
		
		/**
		 * Applies the parameters represented by this to a writer.
		 * Undefined parameters are not set, leaving the lucene default.
		 * @param writer the IndexWriter whereto the parameters will be applied.
		 */
		public void applyToWriter(IndexWriter writer) {
			for ( Map.Entry<IndexWriterSetting,Integer> entry : parameters.entrySet() ) {
				try {
					entry.getKey().applySetting( writer, entry.getValue() );
				} catch ( IllegalArgumentException e ) {
					//TODO if DirectoryProvider had getDirectoryName() exceptions could tell better
					throw new SearchException( "Illegal IndexWriter setting "
							+ entry.getKey().getKey() + " "+ e.getMessage(), e );
				}
			}
		}
		
		public Integer getCurrentValueFor(IndexWriterSetting ws){
			return parameters.get(ws);
		}

		public void setCurrentValueFor(IndexWriterSetting ws, Integer newValue){
			if (newValue==null){
				parameters.remove(ws);
			} else {
				parameters.put(ws, newValue);
			}
		}

 	}

}
