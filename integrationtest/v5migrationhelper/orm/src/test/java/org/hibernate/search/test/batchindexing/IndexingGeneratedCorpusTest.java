/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.countAll;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.textbuilder.SentenceInventor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * Tests the fullTextSession.createIndexer() API for basic functionality.
 *
 * @author Sanne Grinovero
 */
class IndexingGeneratedCorpusTest {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final int BOOK_NUM = 140;
	private static final int ANCIENTBOOK_NUM = 120;
	private static final int SECRETBOOK_NUM = 20;
	private static final int DVD_NUM = 200;

	private static final SentenceInventor sentenceInventor = new SentenceInventor( 7L, 4000 );

	@RegisterExtension
	public static FullTextSessionBuilder builder = new FullTextSessionBuilder()
			.addAnnotatedClass( Book.class )
			.addAnnotatedClass( Dvd.class )
			.addAnnotatedClass( AncientBook.class )
			.addAnnotatedClass( Nation.class )
			.addAnnotatedClass( SecretBook.class )
			.setProperty( BackendSettings.backendKey( LuceneBackendSettings.THREAD_POOL_SIZE ), "4" );

	@BeforeAll
	public static void setUp() throws Exception {
		createMany( Book.class, BOOK_NUM );
		createMany( Dvd.class, DVD_NUM );
		createMany( AncientBook.class, ANCIENTBOOK_NUM );
		createMany( SecretBook.class, SECRETBOOK_NUM );
		storeAllBooksInNation();
	}

	private static void createMany(Class<? extends TitleAble> entityType, int amount)
			throws InstantiationException, IllegalAccessException {
		FullTextSession fullTextSession = builder.openFullTextSession();
		int totalEntitiesInDB = 0;
		try {
			Transaction tx = fullTextSession.beginTransaction();
			fullTextSession.persist( new Nation( "Italy", "IT" ) );
			tx.commit();
			tx = fullTextSession.beginTransaction();
			for ( int i = 0; i < amount; i++ ) {
				TitleAble instance = entityType.newInstance();
				instance.setTitle( sentenceInventor.nextSentence() );
				//to test for HSEARCH-512 we make all entities share some proxy
				Nation country = fullTextSession.load( Nation.class, 1 );
				instance.setFirstPublishedIn( country );
				fullTextSession.persist( instance );
				totalEntitiesInDB++;
				if ( i % 250 == 249 ) {
					tx.commit();
					fullTextSession.clear();
					System.out.println( "Test preparation: " + totalEntitiesInDB + " entities persisted" );
					tx = fullTextSession.beginTransaction();
				}
			}
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	/**
	 * Adds all stored books to the Nation.
	 * Needed to test for HSEARCH-534 and makes the dataset to index quite bigger.
	 */
	private static void storeAllBooksInNation() {
		FullTextSession fullTextSession = builder.openFullTextSession();
		try {
			Transaction tx = fullTextSession.beginTransaction();
			List<Book> allBooks =
					fullTextSession.createQuery( "select b from " + Book.class.getName() + " b", Book.class ).list();
			Nation italy = fullTextSession.load( Nation.class, 1 );
			italy.getLibrariesHave().addAll( allBooks );
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Test
	void testBatchIndexing() throws InterruptedException, IOException {
		verifyResultNumbers(); //initial count of entities should match expectations
		purgeAll(); // empty indexes
		verifyIsEmpty();
		reindexAll(); // rebuild the indexes
		verifyResultNumbers(); // verify the count match again
		reindexAll(); //tests that purgeAll is automatic:
		verifyResultNumbers(); //..same numbers again
	}

	@Test
	void testCreationOfTheDefaultMassIndexer() throws Exception {
		FullTextSession fullTextSession = builder.openFullTextSession();
		MassIndexer indexer = fullTextSession.createIndexer( Object.class );
		assertThat( indexer ).isNotNull();
	}

	private void reindexAll() throws InterruptedException {
		FullTextSession fullTextSession = builder.openFullTextSession();
		SilentProgressMonitor progressMonitor = new SilentProgressMonitor();
		assertThat( progressMonitor.finished ).isFalse();
		try {
			fullTextSession.createIndexer( Object.class )
					.threadsForSubsequentFetching( 8 )
					.threadsToLoadObjects( 4 )
					.batchSizeToLoadObjects( 30 )
					.progressMonitor( progressMonitor )
					.startAndWait();
		}
		finally {
			fullTextSession.close();
		}
		assertThat( progressMonitor.finished ).isTrue();
	}

	private void purgeAll() {
		FullTextSession fullTextSession = builder.openFullTextSession();
		try {
			Transaction tx = fullTextSession.beginTransaction();
			fullTextSession.purgeAll( Object.class );
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void verifyResultNumbers() {
		assertThat( countByFT( Dvd.class ) ).isEqualTo( DVD_NUM );
		assertThat( countByFT( Book.class ) ).isEqualTo( ANCIENTBOOK_NUM + BOOK_NUM );
		assertThat( countByDatabaseCriteria( Book.class ) ).isEqualTo( ANCIENTBOOK_NUM + BOOK_NUM + SECRETBOOK_NUM );
		assertThat( countByDatabaseCriteria( SecretBook.class ) ).isEqualTo( SECRETBOOK_NUM );
		assertThat( countByFT( AncientBook.class ) ).isEqualTo( ANCIENTBOOK_NUM );
		assertThat( countByFT( AncientBook.class, Book.class, Dvd.class ) ).isEqualTo( DVD_NUM + ANCIENTBOOK_NUM + BOOK_NUM );
		assertThat( countByFT( AncientBook.class, Dvd.class ) ).isEqualTo( DVD_NUM + ANCIENTBOOK_NUM );
	}

	@SuppressWarnings("unchecked")
	private void verifyIsEmpty() {
		assertThat( countByFT( Dvd.class ) ).isZero();
		assertThat( countByFT( Book.class ) ).isZero();
		assertThat( countByFT( AncientBook.class ) ).isZero();
		assertThat( countByFT( AncientBook.class, Book.class, Dvd.class ) ).isZero();
	}

	private int countByFT(Class<? extends TitleAble>... types) {
		Query findAll = new MatchAllDocsQuery();
		int bySize = 0;
		int byResultSize = 0;
		FullTextSession fullTextSession = builder.openFullTextSession();
		try {
			Transaction tx = fullTextSession.beginTransaction();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( findAll, types );
			bySize = fullTextQuery.list().size();
			byResultSize = fullTextQuery.getResultSize();
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
		assertThat( byResultSize ).isEqualTo( bySize );
		return bySize;
	}

	private long countByDatabaseCriteria(Class<? extends TitleAble> type) {
		Session session = builder.openFullTextSession();
		try {
			Transaction tx = session.beginTransaction();
			try {
				Number countAsNumber = countAll( session, type );
				return countAsNumber.longValue();
			}
			finally {
				tx.commit();
			}
		}
		finally {
			session.close();
		}
	}

	private static class SilentProgressMonitor implements MassIndexerProgressMonitor {

		final LongAdder objectsCounter = new LongAdder();

		volatile boolean finished = false;

		@Override
		public void documentsAdded(long increment) {
		}

		@Override
		public void documentsBuilt(int number) {
		}

		@Override
		public void entitiesLoaded(int size) {
		}

		@Override
		public void addToTotalCount(long count) {
			objectsCounter.add( count );
		}

		@Override
		public void indexingCompleted() {
			finished = true;
			log.debug( "Finished indexing " + objectsCounter.doubleValue() + " entities" );
		}
	}
}
