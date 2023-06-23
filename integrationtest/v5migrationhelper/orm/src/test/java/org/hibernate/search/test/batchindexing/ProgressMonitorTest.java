/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.batchindexing;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.progessmonitor.AssertingMassIndexerProgressMonitor;

import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * @author Hardy Ferentschik
 */
public class ProgressMonitorTest extends SearchTestBase {
	FullTextSession fullTextSession;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		initializeData( fullTextSession );
	}

	@Test
	public void testAllRelevantProgressMonitoringOperationsCalled() throws InterruptedException {
		// let mass indexer re-index the data in the db (created in initializeData())
		AssertingMassIndexerProgressMonitor monitor = new AssertingMassIndexerProgressMonitor( 10, 10 );
		fullTextSession.createIndexer( LegacyCar.class )
				.progressMonitor( monitor )
				.startAndWait();
		fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), LegacyCar.class )
				.getResultSize();
		monitor.assertExpectedProgressMade();
	}

	private static void initializeData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LegacyCar[] cars = new LegacyCar[10];
		for ( int i = 0; i < cars.length; i++ ) {
			cars[i] = new LegacyCar();
			cars[i].setId( "" + i );
			cars[i].setModel( "model" + i );
			fullTextSession.persist( cars[i] );
		}
		transaction.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { LegacyCarPlant.class, LegacyCar.class, LegacyTire.class };
	}
}
