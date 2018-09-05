/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public abstract class ReaderPerformance extends SearchTestBase {

	private static final Boolean PERFORMANCE_ENABLED = TestConstants.arePerformanceTestsEnabled();

	//more iterations for more reliable measures:
	private static final int TOTAL_WORK_BATCHES = PERFORMANCE_ENABLED ? 10 : 0;
	//the next 3 define the kind of workload mix to test on:
	private static final int SEARCHERS_PER_BATCH = PERFORMANCE_ENABLED ? 10 : 1;
	private static final int UPDATES_PER_BATCH = 2;
	private static final int INSERTIONS_PER_BATCH = 1;
	private static final int INDEX_ELEMENTS = PERFORMANCE_ENABLED ? 5000000 : 2;

	private static final int WORKER_THREADS = PERFORMANCE_ENABLED ? 20 : 1;

	private static final int WARM_UP_CYCLES = PERFORMANCE_ENABLED ? 6 : 1;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Path indexBase = getIndexBaseDir();
		FileHelper.delete( indexBase );
		Files.createDirectories( indexBase );
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		FileHelper.delete( getIndexBaseDir() );
	}

	@Test
	public final void testPerformance() throws InterruptedException, IOException {
		buildBigIndex();
		for ( int i = 0; i < WARM_UP_CYCLES; i++ ) {
			timeMs();
		}
	}

	private void buildBigIndex() throws InterruptedException, IOException {
		System.out.println( "Going to create fake index..." );
		Path detectiveIndexPath = getIndexBaseDir().resolve( Detective.class.getCanonicalName() );
		FSDirectory directory = FSDirectory.open( detectiveIndexPath );

		SimpleAnalyzer analyzer = new SimpleAnalyzer();
		IndexWriterConfig cfg = new IndexWriterConfig( analyzer );
		IndexWriter iw = new IndexWriter( directory, cfg );
		IndexFillRunnable filler = new IndexFillRunnable( iw );
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( WORKER_THREADS );
		for ( int batch = 0; batch <= INDEX_ELEMENTS; batch++ ) {
			executor.execute( filler );
		}
		executor.shutdown();
		executor.awaitTermination( 600, TimeUnit.SECONDS );
		iw.commit();
		iw.forceMergeDeletes();
		iw.forceMerge( 1 );
		iw.close();
		System.out.println( "Index created." );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Detective.class,
				Suspect.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.put( "hibernate.search.default.indexBase", getIndexBaseDir().toAbsolutePath().toString() );
		cfg.put( "hibernate.search.default.optimizer.transaction_limit.max", "10" ); // prevent too many open files
		cfg.put( "hibernate.search.default." + Environment.EXCLUSIVE_INDEX_USE, "true" );
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.put( Environment.READER_STRATEGY, getReaderStrategyName() );
	}

	protected abstract String getReaderStrategyName();

	protected abstract Path getIndexBaseDir();

	private void timeMs() throws InterruptedException {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( WORKER_THREADS );
		CountDownLatch startSignal = new CountDownLatch( 1 );
		InsertActivity insertionTask = new InsertActivity( getSessionFactory(), startSignal );
		SearchActivity searchTask = new SearchActivity( getSessionFactory(), startSignal );
		UpdateActivity updateTask = new UpdateActivity( getSessionFactory(), startSignal );
		//we declare needed activities in order, scheduler will "mix":
		for ( int batch = 0; batch <= TOTAL_WORK_BATCHES; batch++ ) {
			for ( int inserters = 0; inserters < INSERTIONS_PER_BATCH; inserters++ ) {
				executor.execute( insertionTask );
			}
			for ( int searchers = 0; searchers < SEARCHERS_PER_BATCH; searchers++ ) {
				executor.execute( searchTask );
			}
			for ( int updaters = 0; updaters < UPDATES_PER_BATCH; updaters++ ) {
				executor.execute( updateTask );
			}
		}
		executor.shutdown();
		long startTime = System.nanoTime();
		startSignal.countDown();//start!
		executor.awaitTermination( 600, TimeUnit.SECONDS );
		long endTime = System.nanoTime();
		System.out.println(
				"Performance test for " + getReaderStrategyName() + ": "
						+ TimeUnit.NANOSECONDS.toMillis( endTime - startTime ) + "ms. (" +
						( TOTAL_WORK_BATCHES * SEARCHERS_PER_BATCH ) + " searches, " +
						( TOTAL_WORK_BATCHES * INSERTIONS_PER_BATCH ) + " insertions, " +
						( TOTAL_WORK_BATCHES * UPDATES_PER_BATCH ) + " updates)"
		);
	}
}
