/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.backend;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.backend.lucene.Condition;
import org.hibernate.search.test.backend.lucene.Quote;
import org.hibernate.search.test.backend.lucene.StopTimer;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.util.impl.Executors;
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
@RunWith( Parameterized.class )
public class BackendStressTest {

	/**
	 * Sync or Async
	 */
	private static final Mode mode = Mode.sync;

	/**
	 * Directory used
	 */
	private static final Provider provider = Provider.RAM;

	/**
	 * Index manager
	 */
	private static final IndexManager indexManager = IndexManager.DIRECTORY;

	/**
	 * Threads doing read/updates/deletes
	 */
	private static final int numberOfThreads = 5;

	/**
	 * Total number of indexing works each thread will carry
	 */
	private static final int docsPerThread = 500;

	/**
	 * Percentage of the total work that will be ADD
	 */
	private static final int addPercentage = 100;

	/**
	 * Percentage of the total work that will be UPDATE (0-100).
	 * If addPercentage + updatesPercentage < 100 the remainder
	 * of the operations will be DELETE. Deletes are always done
	 * on previously added documents
	 */
	private static final int updatesPercentage = 0;

	/**
	 * Chunk size, used only for the infinispan directory
	 */
	private static final long chunkSize = 16 * 1024;

	/**
	 * For progress output
	 */
	private static final int printEach = 50;

	/**
	 * Number of times to execute the test
	 */
	private static final int REPEAT = 1;

	/**
	 * Number of Hibernate Search engines to start.
	 * This won't affect the backend, but affects the latency
	 * of storage operations in Infinispan.
	 * Defaults to 4 for Infinispan only as it makes no sense
	 * to have a value different than 1 for other directories.
	 */
	private static final int CLUSTER_NODES = (provider == Provider.INFINISPAN) ? 4 : 1;

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
			.withProperty( "hibernate.search.default.indexwriter.ram_buffer_size", "32" )
			.multipleInstances( CLUSTER_NODES );

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
		SearchIntegrator searchIntegrator = sfHolderSync.getSearchFactory();

		ThreadPoolExecutor executor = Executors.newFixedThreadPool( numberOfThreads, "BackendStressTest" );

		Collection<Future> futures = new ArrayList<>( numberOfThreads );

		StopTimer timer = new StopTimer();

		for ( int i = 0; i < numberOfThreads; i++ ) {
			futures.add( executor.submit( new Task( searchIntegrator ) ) );
		}

		waitForAll( futures );

		assertConditionMet( new MinimumSizeCondition( searchIntegrator ) );

		timer.stop();

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
		private final SearchIntegrator integrator;

		MinimumSizeCondition(SearchIntegrator integrator) {
			this.integrator = integrator;
			expectedSize = workLog.calculateIndexSize();
		}
		@Override
		public boolean evaluate() {
			int size = integrator
					.createHSQuery()
					.luceneQuery( new MatchAllDocsQuery() )
					.targetedEntities( Arrays.<Class<?>>asList( Quote.class ) )
					.queryResultSize();
			System.out.println( "Index size=" + size + ", expected=" + expectedSize );
			return size >= expectedSize;

		}
	}

	class Task implements Runnable {
		private final SearchIntegrator integrator;

		public Task(SearchIntegrator integrator) {
			this.integrator = integrator;
		}

		@Override
		public void run() {
			for ( int i = 1; i <= docsPerThread; i++ ) {
				final Worker worker = integrator.getWorker();
				Work work = workLog.generateNewWork();
				TransactionContextForTest tc = new TransactionContextForTest();
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
