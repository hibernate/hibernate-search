/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.impl.MassIndexerImpl;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.backend.LuceneBackendTestHelpers;
import org.hibernate.search.testsupport.textbuilder.SentenceInventor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests the fullTextSession.createIndexer() API for basic functionality.
 *
 * @author Sanne Grinovero
 */
public class IndexingGeneratedCorpusTest {

	private static final Log log = LoggerFactory.make();

	private static final int BOOK_NUM = 140;
	private static final int ANCIENTBOOK_NUM = 120;
	private static final int SECRETBOOK_NUM = 20;
	private static final int DVD_NUM = 200;

	private static final SentenceInventor sentenceInventor = new SentenceInventor( 7L, 4000 );

	@ClassRule
	public static FullTextSessionBuilder builder = new FullTextSessionBuilder()
			.addAnnotatedClass( Book.class )
			.addAnnotatedClass( Dvd.class )
			.addAnnotatedClass( AncientBook.class )
			.addAnnotatedClass( Nation.class )
			.addAnnotatedClass( SecretBook.class )
			.setProperty( "hibernate.search.DVDS.exclusive_index_use", "false" ) // to test lock release
			.setProperty( "hibernate.search.default.worker.thread_pool.size", "4" );

	@BeforeClass
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
			List<Book> allBooks = fullTextSession.createCriteria( Book.class ).list();
			Nation italy = fullTextSession.load( Nation.class, 1 );
			italy.getLibrariesHave().addAll( allBooks );
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	@Test
	public void testBatchIndexing() throws InterruptedException, IOException {
		verifyResultNumbers(); //initial count of entities should match expectations
		purgeAll(); // empty indexes
		verifyIsEmpty();
		reindexAll(); // rebuild the indexes
		verifyResultNumbers(); // verify the count match again
		reindexAll(); //tests that purgeAll is automatic:
		verifyResultNumbers(); //..same numbers again
		verifyIndexIsLocked( false, Dvd.class ); //non exclusive index configured
		verifyIndexIsLocked( true, Book.class ); //exclusive index enabled
	}

	@Test
	public void testCreationOfTheDefaultMassIndexer() throws Exception {
		FullTextSession fullTextSession = builder.openFullTextSession();
		MassIndexer indexer = fullTextSession.createIndexer( Object.class );
		assertThat( indexer, instanceOf( MassIndexerImpl.class ) );
	}

	private void reindexAll() throws InterruptedException {
		FullTextSession fullTextSession = builder.openFullTextSession();
		SilentProgressMonitor progressMonitor = new SilentProgressMonitor();
		Assert.assertFalse( progressMonitor.finished );
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
		Assert.assertTrue( progressMonitor.finished );
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

	private void verifyIndexIsLocked(boolean isLocked, Class type) throws IOException {
		SearchIntegrator searchIntegrator = builder.getSearchFactory().unwrap( SearchIntegrator.class );
		IndexManager indexManager = searchIntegrator.getIndexBindings().get( type )
				.getIndexManagerSelector().all().iterator().next();

		// No need to check for alternative implementations such as ES
		if ( indexManager instanceof DirectoryBasedIndexManager ) {
			Directory directory = ( (DirectoryBasedIndexManager) indexManager ).getDirectoryProvider().getDirectory();
			Assert.assertEquals( isLocked, LuceneBackendTestHelpers.isLocked( directory ) );
		}
	}

	@SuppressWarnings("unchecked")
	private void verifyResultNumbers() {
		assertEquals(
				DVD_NUM,
				countByFT( Dvd.class )
		);
		assertEquals(
				ANCIENTBOOK_NUM + BOOK_NUM,
				countByFT( Book.class )
		);
		assertEquals(
				ANCIENTBOOK_NUM + BOOK_NUM + SECRETBOOK_NUM,
				countByDatabaseCriteria( Book.class )
		);
		assertEquals(
				SECRETBOOK_NUM,
				countByDatabaseCriteria( SecretBook.class )
		);
		assertEquals(
				ANCIENTBOOK_NUM,
				countByFT( AncientBook.class )
		);
		assertEquals(
				DVD_NUM + ANCIENTBOOK_NUM + BOOK_NUM,
				countByFT( AncientBook.class, Book.class, Dvd.class )
		);
		assertEquals(
				DVD_NUM + ANCIENTBOOK_NUM,
				countByFT( AncientBook.class, Dvd.class )
		);
	}

	@SuppressWarnings("unchecked")
	private void verifyIsEmpty() {
		assertEquals( 0, countByFT( Dvd.class ) );
		assertEquals( 0, countByFT( Book.class ) );
		assertEquals( 0, countByFT( AncientBook.class ) );
		assertEquals( 0, countByFT( AncientBook.class, Book.class, Dvd.class ) );
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
		assertEquals( bySize, byResultSize );
		return bySize;
	}

	private long countByDatabaseCriteria(Class<? extends TitleAble> type) {
		Session session = builder.openFullTextSession();
		try {
			Transaction tx = session.beginTransaction();
			try {
				Number countAsNumber = (Number) session
						.createCriteria( type )
						.setProjection( Projections.rowCount() )
						.uniqueResult();
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
