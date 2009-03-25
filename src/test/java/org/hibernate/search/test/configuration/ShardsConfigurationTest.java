// $Id$
package org.hibernate.search.test.configuration;

import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.RAM_BUFFER_SIZE;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.TERM_INDEX_INTERVAL;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.TRANSACTION;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.BATCH;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;

/**
 * @author Sanne Grinovero
 */
public class ShardsConfigurationTest extends ConfigurationReadTestCase {
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		//super contains these:
//		cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
//		cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
		cfg.setProperty( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );// permit this?
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.default.2.directory_provider", RAMDirectoryProvider.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.batch.max_buffered_docs", "4" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_merge_docs", "5" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_buffered_docs", "6" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.nbr_of_shards", "4" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy", UselessShardingStrategy.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.system.default", "45" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.output", "70" );
		cfg.setProperty( "hibernate.search.Documents.0.batch.max_merge_docs", "57" );
		cfg.setProperty( "hibernate.search.Documents.0.directory_provider", RAMDirectoryProvider.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.0.transaction.max_buffered_docs", "58" );
		cfg.setProperty( "hibernate.search.Documents.1.batch.max_merge_docs", "11" );
		cfg.setProperty( "hibernate.search.Documents.1.transaction.max_buffered_docs", "12" );
		cfg.setProperty( "hibernate.search.Documents.1.transaction.term_index_interval", "12" );
	}
	
	public void testCorrectNumberOfShardsDetected() throws Exception {
		DirectoryProvider[] docDirProviders = getSearchFactory()
			.getDirectoryProviders( Document.class );
		assertNotNull( docDirProviders);
		assertEquals( 4, docDirProviders.length );
		DirectoryProvider[] bookDirProviders = getSearchFactory()
			.getDirectoryProviders( Book.class );
		assertNotNull( bookDirProviders );
		assertEquals( 2, bookDirProviders.length );
	}
	
	public void testSelectionOfShardingStrategy() throws Exception {
		IndexShardingStrategy shardingStrategy = getSearchFactory().getDocumentBuilderIndexedEntity( Document.class )
				.getDirectoryProviderSelectionStrategy();
		assertNotNull( shardingStrategy );
		assertEquals( shardingStrategy.getClass(), UselessShardingStrategy.class );
	}
	
	public void testShardingSettingsInherited() throws Exception {
		DirectoryProvider[] docDirProviders = getSearchFactory().getDirectoryProviders( Document.class );
		assertTrue( docDirProviders[0] instanceof RAMDirectoryProvider );
		assertTrue( docDirProviders[1] instanceof FSDirectoryProvider );
		assertTrue( docDirProviders[2] instanceof RAMDirectoryProvider );
	}
	
	public void testShardN2UsesDefaults() throws Exception {
		assertValueIsSet( Document.class, 2, TRANSACTION, MAX_BUFFERED_DOCS, 6 );
		assertValueIsDefault( Document.class, 2, TRANSACTION, MAX_MERGE_DOCS );
		assertValueIsSet( Document.class, 2, TRANSACTION, MERGE_FACTOR, 100 );
		assertValueIsDefault( Document.class, 2, TRANSACTION, RAM_BUFFER_SIZE );
		assertValueIsSet( Document.class, 2, BATCH, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, BATCH, MAX_MERGE_DOCS, 5 );
		assertValueIsSet( Document.class, 2, BATCH, MERGE_FACTOR, 100 );
		assertValueIsDefault( Document.class, 2, BATCH, RAM_BUFFER_SIZE );
	}
	
	public void testShardN1_ExplicitParams() throws Exception {
		assertValueIsSet( Document.class, 1, TRANSACTION, MAX_BUFFERED_DOCS, 12 );
		assertValueIsSet( Document.class, 1, BATCH, MAX_MERGE_DOCS, 11 );
	}
	
	public void testShard_BatchInheritedFromTransaction() throws Exception {
		assertValueIsSet( Document.class, 1, BATCH, TERM_INDEX_INTERVAL, 12 );
		assertValueIsSet( Document.class, 0, BATCH, MAX_BUFFERED_DOCS, 4 );
	}
	
	protected Class[] getMappings() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}
}
