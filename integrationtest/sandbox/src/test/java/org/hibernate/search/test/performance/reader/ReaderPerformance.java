/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.performance.reader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import org.hibernate.search.Environment;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.impl.FileHelper;

/**
 * @author Sanne Grinovero
 */
public abstract class ReaderPerformance extends SearchTestCase {

	//more iterations for more reliable measures:
	private static final int TOTAL_WORK_BATCHES = 10;
	//the next 3 define the kind of workload mix to test on:
	private static final int SEARCHERS_PER_BATCH = 10;
	private static final int UPDATES_PER_BATCH = 2;
	private static final int INSERTIONS_PER_BATCH = 1;

	private static final int WORKER_THREADS = 20;

	private static final int WARM_UP_CYCLES = 6;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		File baseIndexDir = getBaseIndexDir();
		baseIndexDir.mkdirs();
		File[] files = baseIndexDir.listFiles();
		for ( File file : files ) {
			FileHelper.delete( file );
		}
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		FileHelper.delete( getBaseIndexDir() );
	}

	public final void testPerformance() throws InterruptedException, IOException {
		buildBigIndex();
		for ( int i = 0; i < WARM_UP_CYCLES; i++ ) {
			timeMs();
		}
	}

	private void buildBigIndex() throws InterruptedException, IOException {
		System.out.println( "Going to create fake index..." );
		FSDirectory directory = FSDirectory.open( new File( getBaseIndexDir(), Detective.class.getCanonicalName() ) );
		IndexWriter.MaxFieldLength fieldLength = new IndexWriter.MaxFieldLength( IndexWriter.DEFAULT_MAX_FIELD_LENGTH );
		IndexWriter iw = new IndexWriter( directory, new SimpleAnalyzer(), true, fieldLength );
		IndexFillRunnable filler = new IndexFillRunnable( iw );
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( WORKER_THREADS );
		for ( int batch = 0; batch <= 5000000; batch++ ) {
			executor.execute( filler );
		}
		executor.shutdown();
		executor.awaitTermination( 600, TimeUnit.SECONDS );
		iw.commit();
		iw.optimize();
		iw.close();
		System.out.println( "Index created." );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Detective.class,
				Suspect.class
		};
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		cfg.setProperty(
				"hibernate.search.default.optimizer.transaction_limit.max", "10"
		); // workaround too many open files
		cfg.setProperty( "hibernate.search.default." + Environment.EXCLUSIVE_INDEX_USE, "true" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( Environment.READER_STRATEGY, getReaderStrategyName() );
	}

	protected abstract String getReaderStrategyName();

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
