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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.document.Document;

import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SearcherThread implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class );

	private final int threadId;
	private final String queryString;
	private final SessionFactory sf;
	private final IndexSearcher indexsearcher;
	private final boolean isLucene;
	private final CountDownLatch startSignal;
	private long time;

	/**
	 * Initialize with thread-id, queryString, indexSearcher
	 * @param startSignal
	 */
	public SearcherThread(int threadId, String queryString, SessionFactory sf, IndexSearcher indexSearcher, boolean isLucene, CountDownLatch startSignal) {
		this.isLucene = isLucene;
		this.threadId = threadId;
		this.queryString = queryString;
		this.sf = sf;
		this.indexsearcher = indexSearcher;
		this.startSignal = startSignal;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			startSignal.await();
		}
		catch (InterruptedException e) {
			log.error( "tests canceled", e );
			Thread.currentThread().interrupt();
			return;
		}
		if ( isLucene ) {
			runLucene();
		}
		else {
			runHSearch();
		}
	}

	public void runLucene() {
		try {
			Query q = getQuery();
			long start = System.nanoTime();
			// Search
			TopDocs hits = indexsearcher.search( q, 1000 );
			List<String> names = new ArrayList<String>(100);
			for (int i = 0 ; i < 100 ; i++) {
				Document doc = getDocument( indexsearcher, hits.scoreDocs[i].doc );
				names.add( doc.get( "name" ) );
			}
			int resultSize = hits.totalHits;
			long totalTime = TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start );
//			log.error( "Lucene [ Thread-id : " + threadId + " ] Total time taken for search is : " + totalTime + "ms with total no. of matching records : " + hits.length() );
			setTime( totalTime );
		}
		catch (ParseException e) {
			System.out.println( "[ Thread-id : " + threadId + " ] Parse Exception for queryString : " + queryString );
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println( "[ Thread-id : " + threadId + " ] IO Exception for queryString : " + queryString );
		}
		catch (Exception e) {
			e.printStackTrace( );
		}
	}

	private Document getDocument(IndexSearcher searcher, int docId ) {
		try {
			return searcher.doc( docId );
		}
		catch (IOException ioe) {
			throw new SearchException( "Unable to retrieve document", ioe );
		}
	}

	private Query getQuery() throws ParseException {
		QueryParser qp = new QueryParser( TestConstants.getTargetLuceneVersion(), "t", TestConstants.standardAnalyzer );
		qp.setLowercaseExpandedTerms( true );
		// Parse the query
		Query q = qp.parse( queryString );
		if ( q instanceof BooleanQuery ) {
			BooleanQuery.setMaxClauseCount( Integer.MAX_VALUE );
		}
		return q;
	}

	public void runHSearch() {
		try {
			Query q = getQuery();
			// Search
			FullTextSession ftSession = Search.getFullTextSession( sf.openSession(  ) );
			final FullTextQuery textQuery = ftSession.createFullTextQuery( q, Boat.class )
					.setMaxResults( 100 ).setProjection( "name" );
			long start = System.nanoTime();
			List results = textQuery.list();
			int resultSize = textQuery.getResultSize();
			long totalTime = TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start );
			ftSession.close();
//			log.error( "HSearch [ Thread-id : " + threadId + " ] Total time taken for search is : " + totalTime + "ms with total no. of matching records : " + resultSize );
			setTime( totalTime );
		}
		catch (ParseException e) {
			log.error( "[ Thread-id : " + threadId + " ] Parse Exception for queryString : " + queryString );
			e.printStackTrace();
		}
		catch (Throwable e) {
			log.error( "[ Thread-id : " + threadId + " ] Exception for queryString : " + queryString );
			e.printStackTrace(  );
		}
	}

	public synchronized long getTime() {
		return time;
	}

	public synchronized void setTime(long time) {
		this.time = time;
	}

}
