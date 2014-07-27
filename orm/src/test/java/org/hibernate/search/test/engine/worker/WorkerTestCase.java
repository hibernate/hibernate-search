/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class WorkerTestCase extends SearchTestBase {

	@Test
	public void testConcurrency() throws Exception {
		final AtomicBoolean allFine = new AtomicBoolean( true );
		int nThreads = 15;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		Work work = new Work( getSessionFactory(), allFine, isWorkerSync() );
		ReverseWork reverseWork = new ReverseWork( getSessionFactory(), allFine );
		long start = System.nanoTime();
		int iteration = 100;
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}
		es.shutdown();
		es.awaitTermination( 100, TimeUnit.MINUTES );
		getSessionFactory().close();
		Assert.assertTrue(
				"Something was wrong in the concurrent threads, please check logs for stacktraces",
				allFine.get()
		);
		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + nThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start )
		);
	}

	protected static final class Work implements Runnable {
		private final SessionFactory sf;
		private final AtomicBoolean allFine;
		private final boolean isWorkerSync;

		public Work(SessionFactory sf, AtomicBoolean allFine, boolean isWorkerSync) {
			this.sf = sf;
			this.allFine = allFine;
			this.isWorkerSync = isWorkerSync;
		}

		@Override
		public void run() {
			Session s = null;
			Transaction tx = null;
			try {
				s = sf.openSession();
				tx = s.beginTransaction();
				Employer er = new Employer();
				er.setName( "RH" );
				s.persist( er );
				Employee ee = new Employee();
				ee.setName( "Emmanuel" );
				s.persist( ee );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				ee = (Employee) s.get( Employee.class, ee.getId() );
				ee.setName( "Emmanuel2" );
				tx.commit();
				s.close();
				s = sf.openSession();
				tx = s.beginTransaction();
				er = (Employer) s.get( Employer.class, er.getId() );
				er.setName( "RH2" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				QueryParser parser = new QueryParser(
						TestConstants.getTargetLuceneVersion(), "id",
						TestConstants.stopAnalyzer
				);
				Query query;
				try {
					query = parser.parse( "name:emmanuel2" );
				}
				catch (ParseException e) {
					throw new RuntimeException( e );
				}
				boolean results = Search.getFullTextSession( s ).createFullTextQuery( query ).list().size() > 0;
				// don't test because in case of async, it query happens before
				// actual saving
				if ( isWorkerSync ) {
					Assert.assertTrue( results );
				}
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				er = (Employer) s.get( Employer.class, er.getId() );
				s.delete( er );
				tx.commit();
				s.close();
				s = sf.openSession();
				tx = s.beginTransaction();
				ee = (Employee) s.get( Employee.class, ee.getId() );
				s.delete( ee );
				tx.commit();
				s.close();
			}
			catch (Throwable t) {
				allFine.set( false );
				t.printStackTrace();
			}
			finally {
				try {
					if ( tx != null && tx.isActive() ) {
						tx.rollback();
					}
					if ( s != null && s.isOpen() ) {
						s.close();
					}
				}
				catch (Throwable t) {
					allFine.set( false );
					t.printStackTrace();
				}
			}
		}

	}

	protected static final class ReverseWork implements Runnable {
		private final SessionFactory sf;
		private final AtomicBoolean allFine;

		public ReverseWork(SessionFactory sf, AtomicBoolean allFine) {
			this.sf = sf;
			this.allFine = allFine;
		}

		@Override
		public void run() {
			try {
				Session s = sf.openSession();
				Transaction tx = s.beginTransaction();
				Employer er = new Employer();
				er.setName( "RH" );
				s.persist( er );
				Employee ee = new Employee();
				ee.setName( "Emmanuel" );
				s.persist( ee );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				er = (Employer) s.get( Employer.class, er.getId() );
				er.setName( "RH2" );
				ee = (Employee) s.get( Employee.class, ee.getId() );
				ee.setName( "Emmanuel2" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				er = (Employer) s.get( Employer.class, er.getId() );
				s.delete( er );
				ee = (Employee) s.get( Employee.class, ee.getId() );
				s.delete( ee );
				tx.commit();
				s.close();
			}
			catch (Throwable t) {
				allFine.set( false );
				t.printStackTrace();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
				Employer.class
		};
	}

	protected boolean isWorkerSync() {
		return true;
	}
}
