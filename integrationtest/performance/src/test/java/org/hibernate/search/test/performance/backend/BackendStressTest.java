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
package org.hibernate.search.test.performance.backend;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.backend.lucene.Condition;
import org.hibernate.search.test.backend.lucene.Quote;
import org.hibernate.search.test.backend.lucene.StopTimer;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.SearchFactoryHolder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hibernate.search.test.backend.lucene.Conditions.assertConditionMet;

/**
 * Stress test for backend writing. Supports async and sync modes, different directory providers and also
 * it's possible to tune the percentage of ADD, DELETE and UPDATES done against the index.
 * The goal of this test is to send unitary LuceneWorks produced by several threads.
 *
 * @author gustavonalle
 */
@RunWith(Parameterized.class)
public class BackendStressTest {

	/**
	 * Sync or Async
	 */
	private final Mode mode = Mode.sync;

	/**
	 * Directory used
	 */
	private final Provider provider = Provider.INFINISPAN;

	/**
	 * Index manager
	 */
	private final IndexManager indexManager = IndexManager.NRT;

	/**
	 * Threads doing read/updates/deletes
	 */
	private final int numberOfThreads = 50;

	/**
	 * Total number of indexing works each thread will carry
	 */
	private final int docsPerThread = 10000;

	/**
	 * Percentage of the total work that will be ADD
	 */
	private final int addPercentage = 100;

	/**
	 * Percentage of the total work that will be UPDATE (0-100).
	 * If addPercentage + updatesPercentage < 100 the remainder
	 * of the operations will be DELETE. Deletes are always done
	 * on previously added documents
	 */
	private final int updatesPercentage = 0;

	/**
	 * Chunk size, used only for the infinispan directory
	 */
	private final long chunkSize = 16 * 1024;

	/**
	 * For progress output
	 */
	private final int printEach = 500;

	/**
	 * Number of times to execute the test
	 */
	private static final int REPEAT = 1;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList( new Object[REPEAT][0] );
	}

	private final WorkLog workLog;

	@Rule
	public SearchFactoryHolder sfHolderSync = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.directory_provider", provider.toString() )
			.withProperty( "hibernate.search.default.worker.execution", mode.toString() )
			.withProperty( "hibernate.search.default.indexmanager", indexManager.toString() )
			.withProperty( "hibernate.search.default.chunk_size", String.valueOf( chunkSize ) )
			.withProperty( "hibernate.search.default.indexwriter.merge_factor", "20" )
			.withProperty( "hibernate.search.default.indexwriter.ram_buffer_size", "32" );

	public BackendStressTest() {
		this.workLog = new WorkLog( numberOfThreads * docsPerThread, addPercentage, updatesPercentage );
	}

	private enum Mode {
		async,
		sync
	}

	private enum Provider {
		RAM( "ram" ),
		FILESYSTEM( "filesystem" ),
		INFINISPAN( "infinispan" );
		private final String cfg;

		Provider(String cfg) {
			this.cfg = cfg;
		}

		@Override
		public String toString() {
			return cfg;
		}
	}

	private enum IndexManager {
		NRT( "near-real-time" ),
		DIRECTORY( "directory-based" );
		private final String cfg;

		IndexManager(String cfg) {
			this.cfg = cfg;
		}

		@Override
		public String toString() {
			return cfg;
		}
	}

	@Test
	public void testRun() throws Exception {
		SearchFactoryImplementor searchFactoryImplementor = sfHolderSync.getSearchFactory();

		ThreadPoolExecutor executor = Executors.newFixedThreadPool( numberOfThreads, "BackendStressTest" );

		Collection<Future> futures = new ArrayList<Future>( numberOfThreads );

		StopTimer timer = new StopTimer();

		for ( int i = 0; i < numberOfThreads; i++ ) {
			futures.add( executor.submit( new Task( searchFactoryImplementor ) ) );
		}

		waitForAll( futures );

		assertConditionMet( new MinimumSizeCondition( searchFactoryImplementor ) );

		timer.stop();

		executor.shutdown();

		System.out.println( "Test finished in " + timer.getElapsedIn( SECONDS ) + " seconds" );
	}

	public static void waitForAll(Collection<Future> futures) throws Exception {
		for ( Future<?> future : futures ) {
			future.get();
		}
	}

	/**
	 * Condition that uses the workLog to calculate the expected final number of documents in the index
	 */
	class MinimumSizeCondition implements Condition {
		final int expectedSize;
		private final SearchFactoryImplementor searchFactoryImplementor;

		MinimumSizeCondition(SearchFactoryImplementor searchFactoryImplementor) {
			this.searchFactoryImplementor = searchFactoryImplementor;
			expectedSize = workLog.calculateIndexSize();
		}

		@Override
		public boolean evaluate() {
			int size = searchFactoryImplementor
					.createHSQuery()
					.luceneQuery( new MatchAllDocsQuery() )
					.targetedEntities( Arrays.<Class<?>>asList( Quote.class ) )
					.queryResultSize();
			System.out.println( "Index size=" + size + ", expected=" + expectedSize );
			return size >= expectedSize;

		}
	}

	class Task implements Runnable {
		private final SearchFactoryImplementor searchFactory;

		public Task(SearchFactoryImplementor searchFactory) {
			this.searchFactory = searchFactory;
		}

		@Override
		public void run() {
			for ( int i = 1; i <= docsPerThread; i++ ) {
				final Worker worker = searchFactory.getWorker();
				Work work = workLog.generateNewWork();
				ManualTransactionContext tc = new ManualTransactionContext();
				worker.performWork( work, tc );
				workLog.workApplied( work );
				tc.end();
				if ( i % printEach == 0 ) {
					System.out.println( Thread.currentThread().getName() + " sent " + i );
				}
			}

		}
	}


}
