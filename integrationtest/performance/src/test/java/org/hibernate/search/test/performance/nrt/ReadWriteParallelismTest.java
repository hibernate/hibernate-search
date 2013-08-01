/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.performance.nrt;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1317")
public class ReadWriteParallelismTest {

	private static final int THREAD_NUMBER = 30;

	/**
	 * The values below (as committed in source code) are not right for measurement!
	 * We keep them reasonably low for a reasonably quick feedback from test builds.
	 */
	private static final int WARM_UP_SECONDS = 20;
	private static final int FULL_RUN_SECONDS = 240;

	private static final AtomicBoolean failures = new AtomicBoolean( false );
	private static final AtomicBoolean running = new AtomicBoolean( true );
	private static final AtomicInteger cyclesCompleted = new AtomicInteger( 0 );

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class )
		.withProperty( "hibernate.search.default.indexmanager", "near-real-time" );

	@Test
	public void testPropertiesIndexing() throws InterruptedException {
		SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();
		ThreadPoolExecutor threadPool = Executors.newFixedThreadPool( THREAD_NUMBER, "ReadWriteParallelismTest" );
		for ( int i = 0; i < THREAD_NUMBER; i++ ) {
			threadPool.execute( new Task( searchFactory, i ) );
		}
		threadPool.shutdown();
		//Time to warmup only:
		threadPool.awaitTermination( WARM_UP_SECONDS, TimeUnit.SECONDS );
		System.out.println( "Warmup complete. Start measuring now..");
		//Start measuring:
		cyclesCompleted.set( 0 );
		long startMeasurementTime = System.nanoTime();
		threadPool.awaitTermination( FULL_RUN_SECONDS, TimeUnit.SECONDS );
		int doneCycles = cyclesCompleted.get();
		long endMeasurementTime = System.nanoTime();
		Assert.assertFalse( "Some failure happened in Task execution", failures.get() );
		long totalTime = endMeasurementTime - startMeasurementTime;
		long millisecondsElapsedTime = TimeUnit.MILLISECONDS.convert( totalTime, TimeUnit.NANOSECONDS );
		System.out.println( "Completed " + doneCycles + " in " + millisecondsElapsedTime + " milliseconds" );
		running.set( false );
	}

	private static void writeABook(Integer id, String bookTitle, Worker worker) {
		Book book = new Book();
		book.id = id;
		book.title = bookTitle;
		Work work = new Work( book, book.id, WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		worker.performWork( work, tc );
		tc.end();
	}

	private static void deleteABook(Integer id, Worker worker) {
		Book book = new Book();
		book.id = id;
		Work work = new Work( book, id, WorkType.DELETE, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		worker.performWork( work, tc );
		tc.end();
	}

	private static void verifyMatches(SearchFactoryImplementor searchFactory, int expectedMatches, Query query) {
		List<EntityInfo> queryEntityInfos = searchFactory.createHSQuery()
				.luceneQuery( query )
				.targetedEntities( Arrays.asList( new Class<?>[]{ Book.class } ) )
				.queryEntityInfos();
		Assert.assertEquals( expectedMatches, queryEntityInfos.size() );
	}

	private static class Task implements Runnable {

		private final int threadId;
		private final SearchFactoryImplementor searchFactory;

		public Task(SearchFactoryImplementor searchFactory, int threadId) {
			this.searchFactory = searchFactory;
			this.threadId = threadId;
		}

		@Override
		public void run() {
			final Worker worker = searchFactory.getWorker();
			final String title = "Volume N' " + Integer.toString( threadId );
			final Integer bookId = Integer.valueOf( threadId );
			final Query query = new TermQuery( new Term( "title", title ) );
			try {
				while ( running.get() ) {
					cyclesCompleted.incrementAndGet();
					verifyMatches( searchFactory, 0, query );
					writeABook( bookId, title, worker );
					verifyMatches( searchFactory, 1, query );
					deleteABook( bookId, worker );
					verifyMatches( searchFactory, 0, query );
				}
			}
			catch (RuntimeException re) {
				if ( running.get() ) {
					re.printStackTrace();
					failures.set( true );
				}
			}
		}

	}

	@Indexed(index = "books")
	private static class Book {
		@DocumentId
		Integer id;
		@Field(analyze = Analyze.NO, norms = Norms.NO)
		String title;
	}

}
