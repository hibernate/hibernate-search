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
package org.hibernate.search.test.engine.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.TestConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class WorkerTestCase extends SearchTestCaseJUnit4 {

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
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.show_sql", "false" );
		cfg.setProperty( "hibernate.format_sql", "false" );
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
