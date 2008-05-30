package org.hibernate.search.test.perf;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SearcherThread extends Thread {
	private static Logger log = LoggerFactory.getLogger( SearcherThread.class );
	private int threadId;
	private String queryString;
	private SessionFactory sf;
	private IndexSearcher indexsearcher;
	private boolean isLucene;
	public long time;

	/**
	 * Initialize with thread-id, querystring, indexsearcher
	 */
	public SearcherThread(int threadId, String queryString, SessionFactory sf, IndexSearcher indexSearcher, boolean isLucene) {
		this.isLucene = isLucene;
		this.threadId = threadId;
		this.queryString = queryString;
		this.sf = sf;
		this.indexsearcher = indexSearcher;
	}

	public void run() {
		if ( isLucene ) {
			runLucene();
		}
		else {
			runHSearch();
		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void runLucene() {

		try {
			QueryParser qp = new QueryParser( "t",
					new StandardAnalyzer() );
			qp.setLowercaseExpandedTerms( true );
			// Parse the query
			Query q = qp.parse( queryString );
			if ( q instanceof BooleanQuery ) {
				BooleanQuery
						.setMaxClauseCount( Integer.MAX_VALUE );
			}
			long start = System.currentTimeMillis();
			// Search
			Hits hits = indexsearcher.search( q );
			List<String> names = new ArrayList<String>(100);
			for (int i = 1 ; i <= 100 ; i++) {
				names.add( hits.doc( i ).get( "name" ) );
			}
			long totalTime = System.currentTimeMillis() - start;
			log.error( "Lucene [ Thread-id : " + threadId + " ] Total time taken for search is : " + totalTime + "ms with total no. of matching records : " + hits.length() );
			time = totalTime;
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
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

	public void runHSearch() {

		try {
			QueryParser qp = new QueryParser( "t",
					new StandardAnalyzer() );
			qp.setLowercaseExpandedTerms( true );

			// Parse the query
			Query q = qp.parse( queryString );
			

			// Search
			FullTextSession ftSession = Search.createFullTextSession( sf.openSession(  ) );

			final FullTextQuery textQuery = ftSession.createFullTextQuery( q, Boat.class )
					.setMaxResults( 100 ).setProjection( "name" );
			long start = System.currentTimeMillis();
			List results = textQuery.list();
			long totalTime = System.currentTimeMillis() - start;
			ftSession.close();

			log.error( "HSearch [ Thread-id : " + threadId + " ] Total time taken for search is : " + totalTime + "ms with total no. of matching records : " + textQuery.getResultSize() );
			time = totalTime;
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			log.error( "[ Thread-id : " + threadId + " ] Parse Exception for queryString : " + queryString );
			e.printStackTrace();
		}
		catch (Throwable e) {
			log.error( "[ Thread-id : " + threadId + " ] Exception for queryString : " + queryString );
			e.printStackTrace(  );
		}
	}
}
