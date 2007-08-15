package org.hibernate.search.backend;

/**
 * Wrapper class around the Lucene indexing parameters <i>mergeFactor</i>, <i>maxMergeDocs</i> and
 * <i>maxBufferedDocs</i>.
 * <p>
 * There are two sets of these parameters. One is for regular indexing the other is for batch indexing
 * triggered by <code>FullTextSessoin.index(Object entity)</code>
 * 
 * @author Hardy Ferentschik
 *
 */
public class LuceneIndexingParameters {
	
	private int transactionMergeFactor = 10;
	
	private int transactionMaxMergeDocs = Integer.MAX_VALUE;
	
	private int transactionMaxBufferedDocs = 10;
	
	private int batchMergeFactor = 10;
	
	private int batchMaxMergeDocs = Integer.MAX_VALUE;
	
	private int batchMaxBufferedDocs = 10;
	
	// the defaults settings
	private static final int DEFAULT_MERGE_FACTOR = 10;
	
	private static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;
	
	private static final int DEFAULT_MAX_BUFFERED_DOCS = 10;
	
	/**
	 * Constructor which instantiates a new parameter object with the the default values.
	 */
	public LuceneIndexingParameters() {
		transactionMergeFactor = DEFAULT_MERGE_FACTOR;
		batchMergeFactor = DEFAULT_MERGE_FACTOR;
		transactionMaxMergeDocs = DEFAULT_MAX_MERGE_DOCS;
		batchMaxMergeDocs = DEFAULT_MAX_MERGE_DOCS;
		transactionMaxBufferedDocs = DEFAULT_MAX_BUFFERED_DOCS;
		batchMaxBufferedDocs = DEFAULT_MAX_BUFFERED_DOCS;
	}
	
	public int getTransactionMaxMergeDocs() {
		return transactionMaxMergeDocs;
	}

	public void setTransactionMaxMergeDocs(int transactionMaxMergeDocs) {
		this.transactionMaxMergeDocs = transactionMaxMergeDocs;
	}

	public int getTransactionMergeFactor() {
		return transactionMergeFactor;
	}

	public void setTransactionMergeFactor(int transactionMergeFactor) {
		this.transactionMergeFactor = transactionMergeFactor;
	}

	public int getBatchMaxMergeDocs() {
		return batchMaxMergeDocs;
	}

	public void setBatchMaxMergeDocs(int batchMaxMergeDocs) {
		this.batchMaxMergeDocs = batchMaxMergeDocs;
	}

	public int getBatchMergeFactor() {
		return batchMergeFactor;
	}

	public void setBatchMergeFactor(int batchMergeFactor) {
		this.batchMergeFactor = batchMergeFactor;
	}

	public int getBatchMaxBufferedDocs() {
		return batchMaxBufferedDocs;
	}

	public void setBatchMaxBufferedDocs(int batchMaxBufferedDocs) {
		this.batchMaxBufferedDocs = batchMaxBufferedDocs;
	}

	public int getTransactionMaxBufferedDocs() {
		return transactionMaxBufferedDocs;
	}

	public void setTransactionMaxBufferedDocs(int transactionMaxBufferedDocs) {
		this.transactionMaxBufferedDocs = transactionMaxBufferedDocs;
	}
}
