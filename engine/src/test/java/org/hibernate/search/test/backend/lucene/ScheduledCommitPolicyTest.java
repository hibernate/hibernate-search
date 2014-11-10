/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.backend.lucene;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.ScheduledCommitPolicy;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.CountingErrorHandler;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertTrue;

/**
 * Tests for the scheduled commit policy
 *
 * @author gustavonalle
 */
@RunWith( BMUnitRunner.class )
public class ScheduledCommitPolicyTest {

	private static final int NUMBER_ENTITIES = 100;

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.index_flush_interval", "100" )
			.withProperty( "hibernate.search.default.worker.execution", "async" )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" )
			.withProperty( "hibernate.search.error_handler", CountingErrorHandler.class.getName() );

	@Test
	public void testScheduledCommits() throws Exception {
		writeData( sfAsyncExclusiveIndex, NUMBER_ENTITIES );
		AbstractWorkspaceImpl workspace = sfAsyncExclusiveIndex.extractWorkspace( Quote.class );
		CommitPolicy commitPolicy = workspace.getCommitPolicy();

		assertTrue( commitPolicy instanceof ScheduledCommitPolicy );

		ScheduledCommitPolicy scheduledCommitPolicy = (ScheduledCommitPolicy) commitPolicy;
		ScheduledThreadPoolExecutor scheduledExecutor = (ScheduledThreadPoolExecutor) scheduledCommitPolicy.getScheduledExecutorService();

		assertConditionMet( new TaskExecutedCondition( scheduledExecutor, 1 ) );
	}

	@Test
	@BMRule( targetClass = "org.apache.lucene.index.IndexWriter",
				targetMethod = "commit",
				action = "throw new IOException(\"File not found!\")",
				name = "commitError" )
	public void testErrorHandling() throws Exception {
		writeData( sfAsyncExclusiveIndex, 2 );
		final CountingErrorHandler errorHandler = (CountingErrorHandler) sfAsyncExclusiveIndex.getSearchFactory().getErrorHandler();
		assertConditionMet( new Condition() {
			@Override
			public boolean evaluate() {
				return errorHandler.getCountFor( IOException.class ) >= 2;
			}
		} );
	}

	@Test
	public void testDocVisibility() throws Exception {
		writeData( sfAsyncExclusiveIndex, NUMBER_ENTITIES );
		assertConditionMet( new IndexingFinishedCondition( sfAsyncExclusiveIndex, NUMBER_ENTITIES ) );

		writeData( sfAsyncExclusiveIndex, 10 );
		assertConditionMet( new IndexingFinishedCondition( sfAsyncExclusiveIndex, NUMBER_ENTITIES + 10 ) );

		writeData( sfAsyncExclusiveIndex, 1 );
		assertConditionMet( new IndexingFinishedCondition( sfAsyncExclusiveIndex, NUMBER_ENTITIES + 10 + 1 ) );
	}

	private interface Condition {
		boolean evaluate();
	}

	private class IndexingFinishedCondition implements Condition {
		private final int docs;
		private final SearchFactoryImplementor searchFactory;

		private IndexingFinishedCondition(SearchFactoryHolder searchFactoryHolder, int docs) {
			this.searchFactory = searchFactoryHolder.getSearchFactory();
			this.docs = docs;
		}

		private HSQuery matchAllQuery() {
			return searchFactory
					.createHSQuery()
					.luceneQuery( new MatchAllDocsQuery() )
					.targetedEntities( Arrays.<Class<?>>asList( Quote.class ) );
		}

		@Override
		public boolean evaluate() {
			return docs == matchAllQuery().queryResultSize();
		}
	}

	private class TaskExecutedCondition implements Condition {

		private final ScheduledThreadPoolExecutor executor;
		private final int taskCount;

		private TaskExecutedCondition(ScheduledThreadPoolExecutor executor, int taskCount) {
			this.executor = executor;
			this.taskCount = taskCount;
		}

		@Override
		public boolean evaluate() {
			return executor.getCompletedTaskCount() >= taskCount;
		}
	}

	private void assertConditionMet(Condition condition) throws InterruptedException {
		int maxLoops = 10;
		int loop = 0;
		int sleep = 1000;
		while ( ! condition.evaluate() ) {
			Thread.sleep( sleep );
			if ( ++ loop > maxLoops ) {
				throw new AssertionFailure( "Condition not met because of a timeout" );
			}
		}
	}

	private void writeData(SearchFactoryHolder sfHolder, int numberEntities) {
		for ( int i = 0;i < numberEntities;i++ ) {
			Quote quote = new Quote( 1, i * 10 );
			Work<Quote> work = new Work<Quote>( quote, quote.id, WorkType.ADD, false );
			ManualTransactionContext tc = new ManualTransactionContext();
			sfHolder.getSearchFactory().getWorker().performWork( work, tc );
			tc.end();
		}
	}

}
