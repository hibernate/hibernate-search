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
package org.hibernate.search.test.perf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.textui.TestRunner;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class IndexTestDontRun extends SearchTestCase {

	private static final int TOTAL_SEARCHES = 800;
	private static final int SEARCH_THREADS = 100;

	public static void main(String[] args) {
		//isLucene = Boolean.parseBoolean( args[0] );
		TestRunner.run( IndexTestDontRun.class );
	}

	public void notestInit() throws Exception {
		long time = System.nanoTime();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		for (int i = 0; i < 50000; i++) {
			s.save( new Boat( "Maria el Seb", "a long" + i + " description of the land" + i ) );
		}
		tx.commit();
		s.close();
		System.out.println( " init time = " + TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - time ) );
	}

	public void testPerformance() throws Exception {
		measure( true );// JVM warmup
		measure( false );// JVM warmup
		long measureLucene = measure( true );
		long measureSearch = measure( false );
		System.out.println( "Totaltime Lucene = " + measureLucene );
		System.out.println( "Totaltime Search = " + measureSearch );
	}

	public long measure(boolean plainLucene) throws Exception {
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( SEARCH_THREADS );
		threadPool.prestartAllCoreThreads();
		CountDownLatch startSignal = new CountDownLatch(1);
		List<SearcherThread> threadsList = new ArrayList<SearcherThread>( TOTAL_SEARCHES );
		IndexSearcher indexSearcher = getNewSearcher();
		for (int i = 0; i < TOTAL_SEARCHES; i++) {
			// Create a thread and invoke it
			//if ( i % 100 == 0) indexSearcher = getNewSearcher();
			SearcherThread searcherThread = new SearcherThread( i, "name:maria OR description:long" + i, getSessions(), indexSearcher, plainLucene, startSignal );
			threadsList.add( searcherThread );
			threadPool.execute( searcherThread );
		}
		threadPool.shutdown();//required to enable awaitTermination functionality
		startSignal.countDown();//start all created threads
		boolean terminationOk = threadPool.awaitTermination( 60, TimeUnit.SECONDS );
		if ( terminationOk == false ) {
			System.out.println( "No enough time to complete the tests!" );
			return 0;
		}
		long totalTime = 0;
		for (SearcherThread t : threadsList) totalTime += t.getTime();
		return totalTime;
	}

	private IndexSearcher getNewSearcher() {
		final org.hibernate.Session session = getSessions().openSession();
		IndexReader indexReader = Search.getFullTextSession( session ).getSearchFactory().getIndexReaderAccessor().open( Boat.class );
		IndexSearcher indexsearcher = new IndexSearcher( indexReader );
		return indexsearcher;
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Boat.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
//		cfg.setProperty( "hibernate.search.reader.strategy", DumbSharedReaderProvider.class.getName() );
	}
}
