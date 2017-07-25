/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.IndexShardingStrategyIndexManagerSelector;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.RAM_BUFFER_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Sanne Grinovero
 */
public class ShardsConfigurationTest extends ConfigurationReadTestCase {

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );

		cfg.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );// permit this?
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.put( "hibernate.search.default.2.directory_provider", "local-heap" );
		cfg.put( "hibernate.search.Documents.indexwriter.max_buffered_docs", "4" );
		cfg.put( "hibernate.search.Documents.indexwriter.max_merge_docs", "5" );
		cfg.put( "hibernate.search.Documents.sharding_strategy.nbr_of_shards", "4" );
		cfg.put( "hibernate.search.Documents.sharding_strategy", UselessShardingStrategy.class.getCanonicalName() );
		cfg.put( "hibernate.search.Documents.sharding_strategy.test.system.default", "45" );
		cfg.put( "hibernate.search.Documents.sharding_strategy.test.output", "70" );
		cfg.put( "hibernate.search.Documents.0.indexwriter.max_merge_docs", "57" );
		//use fqcn to make sure it still works even after the introduction of the shortcuts
		cfg.put( "hibernate.search.Documents.0.directory_provider", RAMDirectoryProvider.class.getCanonicalName() );
		cfg.put( "hibernate.search.Documents.0.indexwriter.max_buffered_docs", "58" );
		cfg.put( "hibernate.search.Documents.1.indexwriter.max_merge_docs", "11" );
		cfg.put( "hibernate.search.Documents.1.indexwriter.max_buffered_docs", "12" );
		cfg.put( "hibernate.search.Documents.1.indexwriter.term_index_interval", "12" );

		//super contains these:
		//cfg.put( "hibernate.search.default.indexwriter.merge_factor", "100" );
		//cfg.put( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
	}

	@Test
	public void testCorrectNumberOfShardsDetected() {
		EntityIndexBinding indexBindingForDocument = getExtendedSearchIntegrator().getIndexBindings().get( Document.class );
		Set<IndexManager> documentManagers = indexBindingForDocument.getIndexManagerSelector().all();
		assertNotNull( documentManagers );
		assertEquals( 4, documentManagers.size() );
		EntityIndexBinding indexBindingForBooks = getExtendedSearchIntegrator().getIndexBindings().get( Book.class );
		Set<IndexManager> bookManagers = indexBindingForBooks.getIndexManagerSelector().all();
		assertNotNull( bookManagers );
		assertEquals( 2, bookManagers.size() );
	}

	@Test
	public void testSelectionOfShardingStrategy() {
		IndexManagerSelector selector = getExtendedSearchIntegrator().getIndexBindings().get( Document.class ).getIndexManagerSelector();
		assertNotNull( selector );
		assertEquals( selector.getClass(), IndexShardingStrategyIndexManagerSelector.class );
		assertEquals( 4, selector.all().size() );
		assertEquals(
				"Expected the useless strategy to be used (never returns any shard)",
				0,
				selector.forExisting( new PojoIndexedTypeIdentifier( Document.class ), null, null ).size()
		);
	}

	@Test
	@Category(SkipOnElasticsearch.class) // DirectoryProviders and IndexWriterSettings are specific to the Lucene backend
	public void testShardingSettingsInherited() {
		EntityIndexBinding binding = getExtendedSearchIntegrator().getIndexBindings().get( Document.class );
		assertTrue( getDirectoryProvider( getIndexManager( binding, 0 ) ) instanceof RAMDirectoryProvider );
		assertTrue( getDirectoryProvider( getIndexManager( binding, 1 ) ) instanceof FSDirectoryProvider );
		assertTrue( getDirectoryProvider( getIndexManager( binding, 2 ) ) instanceof RAMDirectoryProvider );
		assertValueIsSet( Document.class, 0, MAX_BUFFERED_DOCS, 58 );
		assertValueIsSet( Document.class, 1, MAX_BUFFERED_DOCS, 12 );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // IndexWriterSettings are specific to the Lucene backend
	public void testShardN2UsesDefaults() {
		assertValueIsSet( Document.class, 2, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, MERGE_FACTOR, 100 );
		assertValueIsDefault( Document.class, 2, RAM_BUFFER_SIZE );
		assertValueIsSet( Document.class, 2, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, MAX_MERGE_DOCS, 5 );
		assertValueIsDefault( Document.class, 2, RAM_BUFFER_SIZE );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // IndexWriterSettings are specific to the Lucene backend
	public void testShardN1_ExplicitParams() {
		assertValueIsSet( Document.class, 1, MAX_BUFFERED_DOCS, 12 );
		assertValueIsSet( Document.class, 1, MAX_MERGE_DOCS, 11 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}

	private static DirectoryProvider getDirectoryProvider(IndexManager indexManager) {
		DirectoryBasedIndexManager dpBasedManager = (DirectoryBasedIndexManager) indexManager;
		return dpBasedManager.getDirectoryProvider();
	}
}
