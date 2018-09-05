/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.optimizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.hibernate.search.test.util.TargetDirHelper;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class OptimizerPerformanceTest extends SearchTestBase {

	private static final Path indexDir = TestConstants.getIndexDirectory( TargetDirHelper.getTargetDir() );

	@Override
	@Before
	public void setUp() throws Exception {
		FileHelper.delete( indexDir );
		Files.createDirectories( indexDir );
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		FileHelper.delete( indexDir );
	}

	@Test
	public void testConcurrency() throws Exception {
		int nThreads = PERFORMANCE_TESTS_ENABLED ? 15 : 1;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		final AtomicBoolean testFailed = new AtomicBoolean( false );
		Work work = new Work( getSessionFactory(), testFailed );
		ReverseWork reverseWork = new ReverseWork( getSessionFactory(), testFailed );
		long start = System.nanoTime();
		int iteration = PERFORMANCE_TESTS_ENABLED ? 100 : 1;
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}

		es.shutdown();
		es.awaitTermination( 2, TimeUnit.HOURS );
		Assert.assertFalse( "Some failure happened in background threads. The first cause should be logged to standard output", testFailed.get() );
		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + nThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start )
		);
	}

	protected static class Work implements Runnable {
		private final SessionFactory sf;
		private final AtomicInteger count = new AtomicInteger( 0 );
		private final AtomicBoolean testFailed;

		public Work(SessionFactory sf, AtomicBoolean testFailed) {
			this.sf = sf;
			this.testFailed = testFailed;
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
				w = s.get( Worker.class, w.getId() );
				w.setName( "Gavin" );
				c = s.get( Construction.class, c.getId() );
				c.setName( "W Hotel" );
				tx.commit();
				s.close();

				try {
					Thread.sleep( 50 );
				}
				catch (InterruptedException e) {
					failure( e );
					Thread.currentThread().interrupt();
					return;
				}

				s = sf.openSession();
				tx = s.beginTransaction();
				FullTextSession fts = Search.getFullTextSession( s );
				QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );
				Query query;
				try {
					query = parser.parse( "name:Gavin" );
				}
				catch (ParseException e) {
					failure( e );
					return;
				}
				boolean results = fts.createFullTextQuery( query ).list().size() > 0;
				//don't test because in case of async, it query happens before actual saving
				//if ( !results ) throw new RuntimeException( "No results!" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = s.get( Worker.class, w.getId() );
				s.delete( w );
				c = s.get( Construction.class, c.getId() );
				s.delete( c );
				tx.commit();
				s.close();
				count.incrementAndGet();
			}
			catch (Throwable t) {
				failure( t );
			}
		}

		private void failure(Throwable e) {
			if ( testFailed.compareAndSet( false, true ) ) {
				//Use a conditional CAS to log only the first error:
				//much more helpful to figure out issues as logs might get
				//out of order.
				e.printStackTrace();
			}
		}
	}

	protected static class ReverseWork implements Runnable {
		private final SessionFactory sf;
		private final AtomicBoolean testFailed;

		public ReverseWork(SessionFactory sf, AtomicBoolean testFailed) {
			this.sf = sf;
			this.testFailed = testFailed;
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
				w = s.get( Worker.class, w.getId() );
				w.setName( "Remi" );
				c = s.get( Construction.class, c.getId() );
				c.setName( "Palais des festivals" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				w = s.get( Worker.class, w.getId() );
				s.delete( w );
				c = s.get( Construction.class, c.getId() );
				s.delete( c );
				tx.commit();
				s.close();
			}
			catch (Throwable t) {
				if ( testFailed.compareAndSet( false, true ) ) {
					t.printStackTrace();
				}
			}
		}
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.indexBase", indexDir.toAbsolutePath().toString() );
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Worker.class,
				Construction.class
		};
	}
}
