/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.CountingErrorHandler;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for the scheduled commit policy
 *
 * @author gustavonalle
 */
@RunWith(BMUnitRunner.class)
@Category(SkipOnElasticsearch.class) // Commit policies are specific to the Lucene backend
public class ScheduledCommitPolicyTest {

	private static final int NUMBER_ENTITIES = 1000;

	private static final Poller POLLER = Poller.milliseconds( 50_000, 20 );

	private int globalIdCounter = 0;

	@Rule
	public final SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.index_flush_interval", "1" )
			.withProperty( "hibernate.search.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" )
			.withProperty( "hibernate.search.error_handler", CountingErrorHandler.class.getName() );

	private final SearchITHelper helper = new SearchITHelper( sfAsyncExclusiveIndex );

	private final IndexedTypeIdentifier testType = PojoIndexedTypeIdentifier.convertFromLegacy( Quote.class );

	@Test
	public void testScheduledCommits() throws Exception {
		writeData( NUMBER_ENTITIES );
		AbstractWorkspaceImpl workspace = sfAsyncExclusiveIndex.extractWorkspace( testType );
		CommitPolicy commitPolicy = workspace.getCommitPolicy();

		assertTrue( commitPolicy instanceof ScheduledCommitPolicy );

		ScheduledCommitPolicy scheduledCommitPolicy = (ScheduledCommitPolicy) commitPolicy;
		ScheduledThreadPoolExecutor scheduledExecutor = (ScheduledThreadPoolExecutor) scheduledCommitPolicy.getScheduledExecutorService();

		POLLER.pollAssertion( () -> assertTaskExecuted( scheduledExecutor, 1 ) );
	}

	@Test
	@BMRule(targetClass = "org.apache.lucene.index.IndexWriter",
			targetMethod = "commit",
			action = "throw new IOException(\"File not found!\")",
			name = "commitError")
	public void testErrorHandlingDuringCommit() throws Exception {
		writeData( 2 );
		final CountingErrorHandler errorHandler = (CountingErrorHandler) sfAsyncExclusiveIndex.getSearchFactory().getErrorHandler();
		POLLER.pollAssertion( () -> Assert.assertTrue( errorHandler.getCountFor( IOException.class ) >= 2 ) );
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.backend.impl.lucene.IndexWriterHolder",
			targetMethod = "commitIndexWriter()",
			action = "throw new NullPointerException(\"Fake internal error\")",
			name = "timerDisruptingError")
	public void testErrorHandlingOnBackgroundThread() throws Exception {
		writeData( 2 );
		final CountingErrorHandler errorHandler = (CountingErrorHandler) sfAsyncExclusiveIndex.getSearchFactory().getErrorHandler();
		// It's going to commit once each millisecond, and produce a failure each time.
		// So "4" is just a random number higher than 0, but high enough to
		// verify that the scheduled task is not being killed at the first failure,
		// and will keep trying.
		POLLER.pollAssertion( () -> Assert.assertTrue( errorHandler.getCountFor( NullPointerException.class ) >= 4 ) );
	}

	@Test
	public void testDocVisibility() throws Exception {
		writeData( NUMBER_ENTITIES );
		POLLER.pollAssertion( () -> assertIndexingFinished( sfAsyncExclusiveIndex, NUMBER_ENTITIES ) );

		writeData( 10 );
		POLLER.pollAssertion( () -> assertIndexingFinished( sfAsyncExclusiveIndex, NUMBER_ENTITIES + 10 ) );

		writeData( 1 );
		POLLER.pollAssertion( () -> assertIndexingFinished( sfAsyncExclusiveIndex, NUMBER_ENTITIES + 10 + 1 ) );
	}

	private void assertIndexingFinished(SearchFactoryHolder searchFactoryHolder, int expectedDocsCount) {
		HSQuery query = searchFactoryHolder.getSearchFactory().createHSQuery( new MatchAllDocsQuery(), Quote.class );
		Assert.assertEquals( expectedDocsCount, query.queryResultSize() );
	}

	private void assertTaskExecuted(ScheduledThreadPoolExecutor executor, int taskCount) {
		Assert.assertTrue( executor.getCompletedTaskCount() >= taskCount );
	}

	private void writeData(int numberEntities) {
		for ( int i = 0; i < numberEntities; i++ ) {
			Integer id = globalIdCounter++;
			Quote quote = new Quote( id, "description" );
			helper.add( quote );
		}
	}

}
