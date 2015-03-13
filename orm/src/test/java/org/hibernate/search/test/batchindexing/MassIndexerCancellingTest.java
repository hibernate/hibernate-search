/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BMUnitRunner.class)
public class MassIndexerCancellingTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make();

	@Test
	public void testMassIndexerCancel() throws InterruptedException {
		FullTextSession fullTextSession = prepareSomeData( this );

		InnerIndexerProgressMonitor monitor = new InnerIndexerProgressMonitor();

		int threadsToLoadObjects = 2;

		Future task = fullTextSession.createIndexer( Book.class ).threadsToLoadObjects( threadsToLoadObjects ).batchSizeToLoadObjects( 1 )
				.progressMonitor( monitor ).purgeAllOnStart( true ).optimizeOnFinish( false ).start();

		// wait 2s to let indexing job start
		Thread.sleep( 1000 );

		// cancel indexing job
		task.cancel( true );

		// wait 2s to let indexing threads correctly ending
		Thread.sleep( 1000 );

		fullTextSession.close();

		// check 2 indexing thread enlisted
		Assert.assertTrue( monitor.getThreadNumber() == threadsToLoadObjects );

		// check all indexing thread are interrupted
		Assert.assertTrue( monitor.massIndexerThreadsAreInterruptedOrDied() );

		// verify index is now containing 2 docs
		verifyIndexIntegrity( 2 );

	}

	class InnerIndexerProgressMonitor extends SimpleIndexingProgressMonitor {

		public final List<Thread> threads = Collections.synchronizedList( new ArrayList<Thread>() );

		public InnerIndexerProgressMonitor() {
			super();
		}

		@Override
		public void documentsBuilt(int number) {
			super.documentsBuilt( number );
			log.debug( "enlist EntityLoader thread [" + Thread.currentThread() + "] and simulate document producer activity" );
			threads.add( Thread.currentThread() );

			while ( true ) {
				// simulate activity until thread interrupted
				if ( Thread.currentThread().isInterrupted() ) {
					log.tracef( "Indexing thread is interrupted : end activity simulation " );
					break;
				}
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

	private void verifyIndexIntegrity(int expectedDocs) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction transaction = fullTextSession.beginTransaction();
			Query q = new MatchAllDocsQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Book.class );

			int resultSize = fullTextQuery.getResultSize();
			assertEquals( expectedDocs, resultSize );

			transaction.commit();
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexwriter.infostream", "true" );
	}

}
