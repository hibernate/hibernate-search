/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-655")
public class MassIndexerCancellingTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make();

	@Test
	public void testMassIndexerCancel() throws InterruptedException {
		FullTextSession fullTextSession = prepareSomeData( this );

		InnerIndexerProgressMonitor monitor = new InnerIndexerProgressMonitor();

		int threadsToLoadObjects = 2;

		Future task = fullTextSession.createIndexer( Book.class )
				.threadsToLoadObjects( threadsToLoadObjects )
				.batchSizeToLoadObjects( 1 )
				.progressMonitor( monitor )
				.purgeAllOnStart( true )
				.optimizeOnFinish( false )
				.start();

		// wait a moment to let indexing job start
		monitor.waitUntilBusyThreads( threadsToLoadObjects );

		// cancel indexing job
		task.cancel( true );

		// wait a bit to let indexing threads correctly end
		monitor.waitUntilBusyThreads( 0 );

		fullTextSession.close();

		// check 2 indexing thread enlisted
		Assert.assertTrue( monitor.getThreadNumber() == threadsToLoadObjects );

		// verify index is now containing 2 docs
		Poller.milliseconds( 10_000, 10 ).pollAssertion( () -> {
				Assert.assertEquals( "Expected index size still not reached after 10 seconds!",
						2, getIndexSize() );
		} );

		// check all indexing thread are interrupted
		Assert.assertTrue( monitor.massIndexerThreadsAreInterruptedOrDied() );

	}

	class InnerIndexerProgressMonitor extends SimpleIndexingProgressMonitor {

		public final List<Thread> threads = Collections.synchronizedList( new ArrayList<Thread>() );
		private final AtomicInteger busyThreads = new AtomicInteger();

		public InnerIndexerProgressMonitor() {
			super();
		}

		@Override
		public void documentsBuilt(int number) {
			super.documentsBuilt( number );
			log.debug( "enlist EntityLoader thread [" + Thread.currentThread() + "] and simulate document producer activity" );
			threads.add( Thread.currentThread() );

			busyThreads.incrementAndGet();
			while ( true ) {
				// simulate activity until thread interrupted
				if ( Thread.currentThread().isInterrupted() ) {
					log.tracef( "Indexing thread is interrupted : end activity simulation " );
					break;
				}
			}
			busyThreads.decrementAndGet();
		}

		void waitUntilBusyThreads(int expectedNumberBusyThreads) {
			while ( busyThreads.get() != expectedNumberBusyThreads ) {
				Thread.yield();
			}
		}

		public boolean massIndexerThreadsAreInterruptedOrDied() {
			for ( Thread th : threads ) {
				if ( th.isAlive() ) {
					if ( !th.isInterrupted() ) {
						log.tracef( "Thread [" + th + "] is not interrupted or alive" );
						return false;
					}
				}
			}
			return true;
		}

		public int getThreadNumber() {
			return threads.size();
		}

	}

	private int getIndexSize() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new MatchAllDocsQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Book.class );

			int resultSize = fullTextQuery.getResultSize();

			transaction.commit();

			return resultSize;
		}
		finally {
			fullTextSession.close();
		}
	}

	static FullTextSession prepareSomeData(SearchTestBase testCase) {
		FullTextSession fullTextSession = Search.getFullTextSession( testCase.openSession() );
		fullTextSession.beginTransaction();
		Nation france = new Nation( "France", "FR" );
		fullTextSession.save( france );

		Book ceylonBook = new Book();
		ceylonBook.setTitle( "Ceylon in Action" );
		ceylonBook.setFirstPublishedIn( france );
		fullTextSession.save( ceylonBook );

		Book hsBook = new Book();
		hsBook.setTitle( "HibernateSearch in Action" );
		hsBook.setFirstPublishedIn( france );
		fullTextSession.save( hsBook );

		Book thirdBook = new Book();
		thirdBook.setTitle( "Le chateau de ma mère" );
		thirdBook.setFirstPublishedIn( france );
		fullTextSession.save( thirdBook );

		Book fourthBook = new Book();
		fourthBook.setTitle( "La gloire de mon père" );
		fourthBook.setFirstPublishedIn( france );
		fullTextSession.save( fourthBook );

		fullTextSession.getTransaction().commit();
		return fullTextSession;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

}
