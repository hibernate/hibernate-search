/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import org.junit.Test;

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
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );

		cfg.setProperty( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );// permit this?
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( "hibernate.search.default.2.directory_provider", "ram" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.max_buffered_docs", "4" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.max_merge_docs", "5" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.nbr_of_shards", "4" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy", UselessShardingStrategy.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.system.default", "45" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.output", "70" );
		cfg.setProperty( "hibernate.search.Documents.0.indexwriter.max_merge_docs", "57" );
		//use fqcn to make sure it still works even after the introduction of the shortcuts
		cfg.setProperty( "hibernate.search.Documents.0.directory_provider", RAMDirectoryProvider.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.0.indexwriter.max_buffered_docs", "58" );
		cfg.setProperty( "hibernate.search.Documents.1.indexwriter.max_merge_docs", "11" );
		cfg.setProperty( "hibernate.search.Documents.1.indexwriter.max_buffered_docs", "12" );
		cfg.setProperty( "hibernate.search.Documents.1.indexwriter.term_index_interval", "12" );

		//super contains these:
		//cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );
		//cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
	}

	@Test
	public void testCorrectNumberOfShardsDetected() {
		EntityIndexBinding indexBindingForDocument = getExtendedSearchIntegrator().getIndexBinding( Document.class );
		IndexManager[] documentManagers = indexBindingForDocument.getIndexManagers();
		assertNotNull( documentManagers);
		assertEquals( 4, documentManagers.length );
		EntityIndexBinding indexBindingForBooks = getExtendedSearchIntegrator().getIndexBinding( Book.class );
		IndexManager[] bookManagers = indexBindingForBooks.getIndexManagers();
		assertNotNull( bookManagers );
		assertEquals( 2, bookManagers.length );
	}

	@Test
	public void testSelectionOfShardingStrategy() {
		IndexShardingStrategy shardingStrategy = getExtendedSearchIntegrator().getIndexBinding( Document.class ).getSelectionStrategy();
		assertNotNull( shardingStrategy );
		assertEquals( shardingStrategy.getClass(), UselessShardingStrategy.class );
	}

	@Test
	public void testShardingSettingsInherited() {
		IndexManager[] indexManagers = getExtendedSearchIntegrator().getIndexBindings().get( Document.class ).getIndexManagers();
		assertTrue( getDirectoryProvider( indexManagers[0] ) instanceof RAMDirectoryProvider );
		assertTrue( getDirectoryProvider( indexManagers[1] ) instanceof FSDirectoryProvider );
		assertTrue( getDirectoryProvider( indexManagers[2] ) instanceof RAMDirectoryProvider );
		assertValueIsSet( Document.class, 0, MAX_BUFFERED_DOCS, 58 );
		assertValueIsSet( Document.class, 1, MAX_BUFFERED_DOCS, 12 );
	}

	@Test
	public void testShardN2UsesDefaults() {
		assertValueIsSet( Document.class, 2, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, MERGE_FACTOR, 100 );
		assertValueIsDefault( Document.class, 2, RAM_BUFFER_SIZE );
		assertValueIsSet( Document.class, 2, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, MAX_MERGE_DOCS, 5 );
		assertValueIsDefault( Document.class, 2, RAM_BUFFER_SIZE );
	}

	@Test
	public void testShardN1_ExplicitParams() {
		assertValueIsSet( Document.class, 1, MAX_BUFFERED_DOCS, 12 );
		assertValueIsSet( Document.class, 1, MAX_MERGE_DOCS, 11 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
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
