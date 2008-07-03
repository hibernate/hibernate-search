package org.hibernate.search.test.perf;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.SessionFactory;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SearcherThread implements Runnable {
	private static final Logger log = LoggerFactory.getLogger( SearcherThread.class );
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
		} catch (InterruptedException e) {
			log.error( "tests canceled", e );
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
			long start = System.currentTimeMillis();
			// Search
			Hits hits = indexsearcher.search( q );
			List<String> names = new ArrayList<String>(100);
			for (int i = 0 ; i < 100 ; i++) {
				names.add( hits.doc( i ).get( "name" ) );
			}
			int resultSize = hits.length();
			long totalTime = System.currentTimeMillis() - start;
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

	private Query getQuery() throws ParseException {
		QueryParser qp = new QueryParser( "t", new StandardAnalyzer() );
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
			long start = System.currentTimeMillis();
			List results = textQuery.list();
			int resultSize = textQuery.getResultSize();
			long totalTime = System.currentTimeMillis() - start;
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
