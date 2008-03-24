//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;

/**
 * Wrapper class around the Lucene indexing parameters <i>mergeFactor</i>, <i>maxMergeDocs</i>,
 * <i>maxBufferedDocs</i>, <i>termIndexInterval</i>, <i>RAMBufferSizeMB</i>.
 * <p>
 * There are two sets of these parameters. One is for regular indexing the other is for batch indexing
 * triggered by <code>FullTextSessoin.index(Object entity)</code>
 * 
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 *
 */
public class LuceneIndexingParameters implements Serializable {

	private static final Log log = LogFactory.getLog( LuceneIndexingParameters.class );
	
	private final ParameterSet transactionIndexParameters = new ParameterSet();
	private final ParameterSet batchIndexParameters = new ParameterSet();
	
	/**
	 * Constructor which instantiates new parameter objects with the the default values.
	 */
	public LuceneIndexingParameters() {
		//FIXME: I would recommend setting the following parameters as defaults for batch indexing:
		//batchIndexParameters.setMaxBufferedDocs(null);
		//batchIndexParameters.setRamBufferSizeMB(64);

	}
	
	public ParameterSet getTransactionIndexParameters() {
		return transactionIndexParameters;
	}

	public ParameterSet getBatchIndexParameters() {
		return batchIndexParameters;
	}

	public class ParameterSet implements Serializable {

		private Integer mergeFactor = null;
		private Integer maxMergeDocs = null;
		private Integer maxBufferedDocs = null;
		private Integer termIndexInterval = null;
		private Integer ramBufferSizeMB = null;

		public Integer getMergeFactor() {
			return mergeFactor;
		}
		public void setMergeFactor(Integer mergeFactor) {
			this.mergeFactor = mergeFactor;
		}
		public Integer getMaxMergeDocs() {
			return maxMergeDocs;
		}
		public void setMaxMergeDocs(Integer maxMergeDocs) {
			this.maxMergeDocs = maxMergeDocs;
		}
		public Integer getMaxBufferedDocs() {
			return maxBufferedDocs;
		}
		public void setMaxBufferedDocs(Integer maxBufferedDocs) {
			this.maxBufferedDocs = maxBufferedDocs;
		}
		public Integer getRamBufferSizeMB() {
			return ramBufferSizeMB;
		}
		public void setRamBufferSizeMB(Integer ramBufferSizeMB) {
			this.ramBufferSizeMB = ramBufferSizeMB;
		}
		public Integer getTermIndexInterval() {
			return termIndexInterval;
		}
		public void setTermIndexInterval(Integer termIndexInterval) {
			this.termIndexInterval = termIndexInterval;
		}

		/**
		 * Applies the parameters represented by this to a writer.
		 * Undefined parameters are not set, leaving the lucene default.
		 * @param writer the IndexWriter whereto the parameters will be applied.
		 */
		void applyToWriter(IndexWriter writer){
			try {
			if (mergeFactor!=null)
				writer.setMergeFactor(mergeFactor);
			if (maxMergeDocs!=null)
				writer.setMaxMergeDocs(maxMergeDocs);
			if (maxBufferedDocs!=null)
				writer.setMaxBufferedDocs(maxBufferedDocs);
			if (ramBufferSizeMB!=null)
				writer.setRAMBufferSizeMB(ramBufferSizeMB);
			if (termIndexInterval!=null)
				writer.setTermIndexInterval(termIndexInterval);
			}catch (IllegalArgumentException e) {
				log.error("Illegal IndexWriter setting"+e.getMessage()+". Will use default settings!");
			}
		}

 	}
}
