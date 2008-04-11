//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.IndexWriterSetting;

/**
 * Wrapper class around the Lucene indexing parameters <i>mergeFactor</i>, <i>maxMergeDocs</i>,
 * <i>maxBufferedDocs</i>, <i>termIndexInterval</i>, <i>RAMBufferSizeMB</i>.
 * <p>
 * There are two sets of these parameters. One is for regular indexing the other is for batch indexing
 * triggered by <code>FullTextSessoin.index(Object entity)</code>
 * 
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class LuceneIndexingParameters implements Serializable {

	private static final Log log = LogFactory.getLog( LuceneIndexingParameters.class );

	// value keyword
	public static final String EXPLICIT_DEFAULT_VALUE = "default"; 
	// property path keywords
	public static final String BATCH = "batch";
	public static final String TRANSACTION = "transaction";
	
	private final ParameterSet transactionIndexParameters;
	private final ParameterSet batchIndexParameters;
	
	public LuceneIndexingParameters( Properties sourceProps ) {
		Properties transactionProps = new Properties();
		Properties batchProps = new Properties( transactionProps ); // transaction settings is the default for batch
		//don't iterate on property entries we know all the keys:
		for ( IndexWriterSetting t : IndexWriterSetting.values() ) {
			String key = t.getKey();
			String trxValue = sourceProps.getProperty( TRANSACTION + "." + key );
			if (trxValue != null) {
				transactionProps.setProperty( key, trxValue );
			}
			String batchValue = sourceProps.getProperty( BATCH + "." + key );
			if (batchValue != null) {
				batchProps.setProperty( key, batchValue );
			}
		}
		transactionIndexParameters = new ParameterSet(transactionProps);
		batchIndexParameters = new ParameterSet(batchProps);
	}

	public ParameterSet getTransactionIndexParameters() {
		return transactionIndexParameters;
	}

	public ParameterSet getBatchIndexParameters() {
		return batchIndexParameters;
	}

	public class ParameterSet implements Serializable {
		
		final Map<IndexWriterSetting, Integer> parameters = new HashMap<IndexWriterSetting, Integer>();
		
		public ParameterSet(Properties prop) {
			for ( IndexWriterSetting t : IndexWriterSetting.values() ) {
				String value = prop.getProperty( t.getKey() );
				if ( ! (value==null || EXPLICIT_DEFAULT_VALUE.equals(value) ) ) {
					parameters.put( t, t.parseVal(value) );
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
