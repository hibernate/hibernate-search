/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.batchindexing;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.assertj.core.api.Assertions;

public class SimpleIndexingProgressMonitorTest extends SearchTestBase {
	private static final int NUMBER_OF_CARS = 150;
	private static final int MASS_INDEXING_MONITOR_LOG_PERIOD = 50; // This is the default in the implementation, do not change this value
	static {
		if ( NUMBER_OF_CARS < 2 * MASS_INDEXING_MONITOR_LOG_PERIOD ) {
			throw new IllegalStateException(
					"There's a bug in tests: NUMBER_OF_CARS should be strictly higher than two times "
							+ MASS_INDEXING_MONITOR_LOG_PERIOD
			);
		}
	}

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3462")
	public void testLoggedMessages() throws InterruptedException {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			initializeData( fullTextSession );

			MassIndexer indexer = fullTextSession.createIndexer();
			/*
			 * The default period for logging in the default mass indexing monitor is 50.
			 * We set the batch size to 49.
			 * 50 = 5*5*2
			 * 49 = 7*7
			 * Thus a multiple of 49 cannot be a multiple of 50,
			 * and if we set the batch size to 49, the bug described in HSEARCH-3462
			 * will prevent any log from ever happening, except at the very end
			 *
			 * Regardless of this bug, here we also check that the mass indexing monitor works correctly:
			 * the number of log events should be equal to NUMBER_OF_BOOKS / 50.
			 */
			int batchSize = 49;
			indexer.batchSizeToLoadObjects( batchSize );
			int expectedNumberOfLogs = NUMBER_OF_CARS / MASS_INDEXING_MONITOR_LOG_PERIOD;
			logged.expectMessage( "documents indexed in" ).times( expectedNumberOfLogs );
			logged.expectMessage( "Indexing speed: " ).times( expectedNumberOfLogs );

			indexer.startAndWait();
		}

		checkEverythingIsIndexed();
	}

	private void checkEverythingIsIndexed() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			int resultSize = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), LegacyCar.class )
					.getResultSize();
			Assertions.assertThat( resultSize ).isEqualTo( NUMBER_OF_CARS );
		}
	}

	private static void initializeData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LegacyCar[] cars = new LegacyCar[NUMBER_OF_CARS];
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
