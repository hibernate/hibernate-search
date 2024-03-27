/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
abstract class WorkerTestCase extends SearchTestBase {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	void testConcurrency() throws Exception {
		int numberOfThreads = 15;
		int iteration = 100;

		Work work = new Work( getSessionFactory(), isWorkerSync() );
		ReverseWork reverseWork = new ReverseWork( getSessionFactory() );

		long start = System.nanoTime();

		// Expect 0 failure in the backend threads
		logged.expectLevelMissing( Level.ERROR );

		new ConcurrentRunner(
				iteration * 2,
				numberOfThreads,
				i -> ( i % 2 == 0 ) ? work : reverseWork
		)
				// in some machine (e.g. Mac) this call may take more than 2 minutes
				.setTimeout( 3, TimeUnit.MINUTES )
				.execute();

		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + numberOfThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) + "ms"
		);
	}

	protected static final class Work implements Runnable {
		private final SessionFactory sf;
		private final boolean isWorkerSync;

		public Work(SessionFactory sf, boolean isWorkerSync) {
			this.sf = sf;
			this.isWorkerSync = isWorkerSync;
		}

		@Override
		public void run() {
			RuntimeException exception = null;
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
				ee.setName( "John" );
				tx.commit();
				s.close();
				s = sf.openSession();
				tx = s.beginTransaction();
				er = (Employer) s.get( Employer.class, er.getId() );
				er.setName( "JBoss" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				Query query = new TermQuery( new Term( "name", "john" ) );
				boolean results = Search.getFullTextSession( s ).createFullTextQuery( query ).list().size() > 0;
				// don't test because in case of async, it query happens before
				// actual saving
				if ( isWorkerSync ) {
					assertThat( results ).isTrue();
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
			catch (RuntimeException e) {
				exception = e;
			}
			finally {
				if ( tx != null && tx.getStatus() == TransactionStatus.ACTIVE ) {
					exception = tryClose( exception, tx::rollback );
				}
				if ( s != null && s.isOpen() ) {
					exception = tryClose( exception, s::close );
				}
			}
			if ( exception != null ) {
				throw exception;
			}
		}

	}

	protected static final class ReverseWork implements Runnable {
		private final SessionFactory sf;

		public ReverseWork(SessionFactory sf) {
			this.sf = sf;
		}

		@Override
		public void run() {
			RuntimeException exception = null;
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
			catch (RuntimeException e) {
				exception = e;
			}
			finally {
				if ( tx != null && tx.getStatus() == TransactionStatus.ACTIVE ) {
					exception = tryClose( exception, tx::rollback );
				}
				if ( s != null && s.isOpen() ) {
					exception = tryClose( exception, s::close );
				}
			}
			if ( exception != null ) {
				throw exception;
			}
		}
	}

	private static RuntimeException tryClose(RuntimeException exception, Runnable runnable) {
		try {
			runnable.run();
			return exception;
		}
		catch (RuntimeException e) {
			if ( exception != null ) {
				exception.addSuppressed( e );
				return exception;
			}
			else {
				return e;
			}
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
				Employer.class
		};
	}

	protected boolean isWorkerSync() {
		return true;
	}
}
