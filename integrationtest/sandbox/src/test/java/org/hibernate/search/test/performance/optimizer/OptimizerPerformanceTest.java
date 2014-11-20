/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.optimizer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class OptimizerPerformanceTest extends SearchTestBase {
	@Override
	@Before
	public void setUp() throws Exception {
		forceConfigurationRebuild();
		String indexBase = TestConstants.getIndexDirectory( OptimizerPerformanceTest.class );
		File indexDir = new File(indexBase);
		FileHelper.delete( indexDir );
		indexDir.mkdirs();
		File[] files = indexDir.listFiles();
		for ( File file : files ) {
			if ( file.isDirectory() ) {
				FileHelper.delete( file );
			}
		}
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		String indexBase = TestConstants.getIndexDirectory( OptimizerPerformanceTest.class );
		File indexDir = new File(indexBase);
		FileHelper.delete( indexDir );
	}

	@Test
	public void testConcurrency() throws Exception {
		int nThreads = PERFORMANCE_TESTS_ENABLED ? 15 : 1;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		Work work = new Work( getSessionFactory() );
		ReverseWork reverseWork = new ReverseWork( getSessionFactory() );
		long start = System.nanoTime();
		int iteration = PERFORMANCE_TESTS_ENABLED ? 100 : 1;
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}

		es.shutdown();

		while ( work.count.get() < iteration - 1 ) {
			Thread.sleep( 20 );
		}
		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + nThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start )
		);
	}

	protected static class Work implements Runnable {
		private final SessionFactory sf;
		public AtomicInteger count = new AtomicInteger( 0 );

		public Work(SessionFactory sf) {
			this.sf = sf;
		}

		@Override
		public void run() {
			try {
				Session s = sf.openSession();
				Transaction tx = s.beginTransaction();
				Worker w = new Worker( "Emmanuel", 65 );
				s.persist( w );
				Construction c = new Construction( "Bellagio", "Las Vagas Nevada" );
				s.persist( c );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = (Worker) s.get( Worker.class, w.getId() );
				w.setName( "Gavin" );
				c = (Construction) s.get( Construction.class, c.getId() );
				c.setName( "W Hotel" );
				tx.commit();
				s.close();

				try {
					Thread.sleep( 50 );
				}
				catch (InterruptedException e) {
					e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
				}

				s = sf.openSession();
				tx = s.beginTransaction();
				FullTextSession fts = Search.getFullTextSession( s );
				QueryParser parser = new QueryParser(
						TestConstants.getTargetLuceneVersion(),
						"id", TestConstants.stopAnalyzer
				);
				Query query;
				try {
					query = parser.parse( "name:Gavin" );
				}
				catch (ParseException e) {
					throw new RuntimeException( e );
				}
				boolean results = fts.createFullTextQuery( query ).list().size() > 0;
				//don't test because in case of async, it query happens before actual saving
				//if ( !results ) throw new RuntimeException( "No results!" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = (Worker) s.get( Worker.class, w.getId() );
				s.delete( w );
				c = (Construction) s.get( Construction.class, c.getId() );
				s.delete( c );
				tx.commit();
				s.close();
				count.incrementAndGet();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	protected static class ReverseWork implements Runnable {
		private SessionFactory sf;

		public ReverseWork(SessionFactory sf) {
			this.sf = sf;
		}

		@Override
		public void run() {
			try {
				Session s = sf.openSession();
				Transaction tx = s.beginTransaction();
				Worker w = new Worker( "Mladen", 70 );
				s.persist( w );
				Construction c = new Construction( "Hover Dam", "Croatia" );
				s.persist( c );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = (Worker) s.get( Worker.class, w.getId() );
				w.setName( "Remi" );
				c = (Construction) s.get( Construction.class, c.getId() );
				c.setName( "Palais des festivals" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = (Worker) s.get( Worker.class, w.getId() );
				s.delete( w );
				c = (Construction) s.get( Construction.class, c.getId() );
				s.delete( c );
				tx.commit();
				s.close();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexBase", TestConstants.getIndexDirectory( OptimizerPerformanceTest.class ) );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Worker.class,
				Construction.class
		};
	}
}
