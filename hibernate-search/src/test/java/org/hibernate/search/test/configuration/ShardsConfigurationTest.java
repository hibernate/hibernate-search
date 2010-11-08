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
package org.hibernate.search.test.configuration;

import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.RAM_BUFFER_SIZE;
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

		cfg.setProperty( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );// permit this?
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( "hibernate.search.default.2.directory_provider", "ram" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_buffered_docs", "4" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_merge_docs", "5" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_buffered_docs", "6" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.nbr_of_shards", "4" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy", UselessShardingStrategy.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.system.default", "45" );
		cfg.setProperty( "hibernate.search.Documents.sharding_strategy.test.output", "70" );
		cfg.setProperty( "hibernate.search.Documents.0.batch.max_merge_docs", "57" );
		//use fqcn to make sure it still works even after the introduction of the shortcuts
		cfg.setProperty( "hibernate.search.Documents.0.directory_provider", RAMDirectoryProvider.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Documents.0.transaction.max_buffered_docs", "58" );
		cfg.setProperty( "hibernate.search.Documents.1.batch.max_merge_docs", "11" );
		cfg.setProperty( "hibernate.search.Documents.1.transaction.max_buffered_docs", "12" );
		cfg.setProperty( "hibernate.search.Documents.1.transaction.term_index_interval", "12" );

		//super contains these:
		//cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		//cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
	}

	public void testCorrectNumberOfShardsDetected() {
		DirectoryProvider[] docDirProviders = getSearchFactory()
			.getDirectoryProviders( Document.class );
		assertNotNull( docDirProviders);
		assertEquals( 4, docDirProviders.length );
		DirectoryProvider[] bookDirProviders = getSearchFactory()
			.getDirectoryProviders( Book.class );
		assertNotNull( bookDirProviders );
		assertEquals( 2, bookDirProviders.length );
	}

	public void testSelectionOfShardingStrategy() {
		IndexShardingStrategy shardingStrategy = getSearchFactory().getDocumentBuilderIndexedEntity( Document.class )
				.getDirectoryProviderSelectionStrategy();
		assertNotNull( shardingStrategy );
		assertEquals( shardingStrategy.getClass(), UselessShardingStrategy.class );
	}

	public void testShardingSettingsInherited() {
		DirectoryProvider[] docDirProviders = getSearchFactory().getDirectoryProviders( Document.class );
		assertTrue( docDirProviders[0] instanceof RAMDirectoryProvider );
		assertTrue( docDirProviders[1] instanceof FSDirectoryProvider );
		assertTrue( docDirProviders[2] instanceof RAMDirectoryProvider );
		assertValueIsSet( Document.class, 0, BATCH, MAX_BUFFERED_DOCS, 4 );
	}

	public void testShardN2UsesDefaults() {
		assertValueIsSet( Document.class, 2, TRANSACTION, MAX_BUFFERED_DOCS, 6 );
		assertValueIsDefault( Document.class, 2, TRANSACTION, MAX_MERGE_DOCS );
		assertValueIsSet( Document.class, 2, TRANSACTION, MERGE_FACTOR, 100 );
		assertValueIsDefault( Document.class, 2, TRANSACTION, RAM_BUFFER_SIZE );
		assertValueIsSet( Document.class, 2, BATCH, MAX_BUFFERED_DOCS, 4 );
		assertValueIsSet( Document.class, 2, BATCH, MAX_MERGE_DOCS, 5 );
		assertValueIsDefault( Document.class, 2, BATCH, MERGE_FACTOR );
		assertValueIsDefault( Document.class, 2, BATCH, RAM_BUFFER_SIZE );
	}

	public void testShardN1_ExplicitParams() {
		assertValueIsSet( Document.class, 1, TRANSACTION, MAX_BUFFERED_DOCS, 12 );
		assertValueIsSet( Document.class, 1, BATCH, MAX_MERGE_DOCS, 11 );
	}

	@Override
	protected void ensureIndexesAreEmpty() {
		// skips index emptying to prevent a problem with UselessShardingStrategy
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}
}
