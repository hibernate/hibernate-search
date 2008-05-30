package org.hibernate.search.test.perf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.textui.TestRunner;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Search;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class IndexTestDontRun extends SearchTestCase {
	private static boolean isLucene;

	public static void main(String[] args) {
		//isLucene = Boolean.parseBoolean( args[0] );
		TestRunner.run( IndexTestDontRun.class );

	}

	public void NonestInit() throws Exception {
		long time = System.currentTimeMillis();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		for (int i = 0; i < 50000; i++) {
			s.save( new Boat( "Maria el Seb", "a long" + i + " description of the land" + i ) );
		}
		tx.commit();
		s.close();
		System.out.println( " init time = " + ( System.currentTimeMillis() - time ) );
	}

	public void testPerf() throws Exception {
		boolean useLucene = false;

		List<SearcherThread> threads = new ArrayList<SearcherThread>( 100 );
		IndexSearcher indexsearcher = getNewSearcher();
		SearcherThread searcherThrea = new SearcherThread( 0, "name:maria OR description:long" + 0, getSessions(), indexsearcher, useLucene );
		searcherThrea.run();
		for (int i = 1; i <= 100; i++) {
			// Create a thread and invoke it
			//if ( i % 100 == 0) indexsearcher = getNewSearcher();
			SearcherThread searcherThread = new SearcherThread( i, "name:maria OR description:long" + i, getSessions(), indexsearcher, useLucene );
			searcherThread.setDaemon( false );
			threads.add( searcherThread );
			searcherThread.start();
		}
		Thread.sleep( 5000 );
		long totalTime = 0;
		for (SearcherThread t : threads) totalTime += t.time;
		System.out.println( "Totaltime=" + totalTime );
	}

	private IndexSearcher getNewSearcher() throws IOException {
		final org.hibernate.classic.Session session = getSessions().openSession();
		Directory d = Search.createFullTextSession( session ).getSearchFactory().getDirectoryProviders( Boat.class )[0].getDirectory();
		IndexSearcher indexsearcher = new IndexSearcher( d );
		return indexsearcher;
	}

	protected Class[] getMappings() {
		return new Class[] {
				Boat.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		//cfg.setProperty( "hibernate.search.reader.strategy", DumbSharedReaderProvider.class.getName() );

	}
}
