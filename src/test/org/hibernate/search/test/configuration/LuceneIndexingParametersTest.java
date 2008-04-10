package org.hibernate.search.test.configuration;

import org.hibernate.search.test.Document;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.RAM_BUFFER_SIZE;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.TRANSACTION;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.BATCH;

/**
 * @author Sanne Grinovero
 */
public class LuceneIndexingParametersTest extends ConfigurationReadTestCase {
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		
		cfg.setProperty( "hibernate.search.default.batch.ram_buffer_size", "1" );
//set by super : cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
		
		cfg.setProperty( "hibernate.search.default.transaction.ram_buffer_size", "2" );
		cfg.setProperty( "hibernate.search.default.transaction.max_merge_docs", "9" );
//set by super : cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.transaction.max_buffered_docs", "11" );
		
		cfg.setProperty( "hibernate.search.Book.batch.max_merge_docs", "12" );
		cfg.setProperty( "hibernate.search.Book.batch.merge_factor", "13" );
		cfg.setProperty( "hibernate.search.Book.batch.max_buffered_docs", "14" );
		
		cfg.setProperty( "hibernate.search.Book.transaction.ram_buffer_size", "4" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_merge_docs", "15" );
		cfg.setProperty( "hibernate.search.Book.transaction.merge_factor", "16" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_buffered_docs", "17" );
		
		cfg.setProperty( "hibernate.search.Documents.transaction.ram_buffer_size", "default" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_merge_docs", "5" );
		cfg.setProperty( "hibernate.search.Documents.transaction.merge_factor", "6" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_buffered_docs", "7" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_merge_docs", "9" );
		
	}
	
	public void testDefaultIndexProviderParameters() throws Exception {
		assertValueIsSet( Author.class, TRANSACTION, RAM_BUFFER_SIZE, 2 );
		assertValueIsSet( Author.class, TRANSACTION, MAX_MERGE_DOCS, 9 );
		assertValueIsSet( Author.class, TRANSACTION, MAX_BUFFERED_DOCS,  11 );
		assertValueIsSet( Author.class, TRANSACTION, MERGE_FACTOR,  100 );
	}
	
	public void testBatchParametersGlobals() throws Exception {
		assertValueIsSet( Author.class, BATCH, RAM_BUFFER_SIZE, 1 );
		assertValueIsSet( Author.class, BATCH, MAX_MERGE_DOCS, 9 );
		assertValueIsSet( Author.class, BATCH, MAX_BUFFERED_DOCS, 1000 );
		assertValueIsSet( Author.class, BATCH, MERGE_FACTOR, 100 );
	}
	
	public void testUnsetBatchValueTakesTransaction() throws Exception {
		assertValueIsSet( Document.class, BATCH, MERGE_FACTOR, 6 );
		assertValueIsSet( Document.class, BATCH, MAX_BUFFERED_DOCS, 7 );
	}
	
	public void testExplicitBatchParameters() throws Exception {
		assertValueIsSet( Book.class, BATCH, MAX_MERGE_DOCS, 12 );
		assertValueIsSet( Book.class, BATCH, MAX_BUFFERED_DOCS, 14 );
		assertValueIsSet( Book.class, BATCH, MERGE_FACTOR, 13 );
	}
	
	public void testInheritedBatchParametersFromTranscation() throws Exception {
		assertValueIsSet( Book.class, BATCH, RAM_BUFFER_SIZE, 4 );
	}
	
	public void testTransactionParameters() throws Exception {
		assertValueIsSet( Book.class, TRANSACTION, RAM_BUFFER_SIZE, 4 );
		assertValueIsSet( Book.class, TRANSACTION, MAX_MERGE_DOCS, 15 );
		assertValueIsSet( Book.class, TRANSACTION, MAX_BUFFERED_DOCS, 17 );
		assertValueIsSet( Book.class, TRANSACTION, MERGE_FACTOR, 16 );
	}
	
	public void testDefaultKeywordOverwritesInherited() throws Exception {
		assertValueIsDefault( Document.class, TRANSACTION, RAM_BUFFER_SIZE );
		assertValueIsDefault( Document.class, TRANSACTION, RAM_BUFFER_SIZE );
	}
	
	protected Class[] getMappings() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}
	
}
