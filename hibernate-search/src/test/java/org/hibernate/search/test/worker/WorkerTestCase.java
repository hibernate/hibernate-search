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
package org.hibernate.search.test.worker;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;

/**
 * @author Emmanuel Bernard
 */
public class WorkerTestCase extends SearchTestCase {
	
	private final AtomicBoolean allFine = new AtomicBoolean( true );

	protected void setUp() throws Exception {
		File sub = getBaseIndexDir();
		sub.mkdir();
		File[] files = sub.listFiles();
		for ( File file : files ) {
			if ( file.isDirectory() ) {
				FileHelper.delete( file );
			}
		}
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		File sub = getBaseIndexDir();
		FileHelper.delete( sub );
		setCfg( null ); //we need a fresh session factory each time for index set up
	}

	public void testConcurrency() throws Exception {
		int nThreads = 15;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		Work work = new Work( getSessions() );
		ReverseWork reverseWork = new ReverseWork( getSessions() );
		long start = System.nanoTime();
		int iteration = 100;
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}
		es.shutdown();
		es.awaitTermination( 100, TimeUnit.MINUTES );
		getSessions().close();
		Assert.assertTrue( "Something was wrong in the concurrent threads, please check logs for stacktraces", allFine.get() );
		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + nThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start )
		);
	}

	protected class Work implements Runnable {
		private SessionFactory sf;

		public Work(SessionFactory sf) {
			this.sf = sf;
		}

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
				ee = ( Employee ) s.get( Employee.class, ee.getId() );
				ee.setName( "Emmanuel2" );
				tx.commit();
				s.close();
				s = sf.openSession();
				tx = s.beginTransaction();
				er = ( Employer ) s.get( Employer.class, er.getId() );
				er.setName( "RH2" );
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				QueryParser parser = new QueryParser(
						getTargetLuceneVersion(), "id",
						SearchTestCase.stopAnalyzer
				);
				Query query;
				try {
					query = parser.parse( "name:emmanuel2" );
				}
				catch ( ParseException e ) {
					throw new RuntimeException( e );
				}
				boolean results = Search.getFullTextSession( s ).createFullTextQuery( query ).list().size() > 0;
				// don't test because in case of async, it query happens before
				// actual saving
				if ( isWorkerSync() ) {
					Assert.assertTrue( results );
				}
				tx.commit();
				s.close();

				s = sf.openSession();
				tx = s.beginTransaction();
				er = ( Employer ) s.get( Employer.class, er.getId() );
				s.delete( er );
				tx.commit();
				s.close();
				s = sf.openSession();
				tx = s.beginTransaction();
				ee = ( Employee ) s.get( Employee.class, ee.getId() );
				s.delete( ee );
				tx.commit();
				s.close();
			}
			catch ( Throwable t ) {
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
				catch ( Throwable t ) {
					allFine.set( false );
					t.printStackTrace();
				}
			}
		}

	}

	protected class ReverseWork implements Runnable {
		private SessionFactory sf;

		public ReverseWork(SessionFactory sf) {
			this.sf = sf;
		}

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
				er = ( Employer ) s.get( Employer.class, er.getId() );
				er.setName( "RH2" );
				ee = ( Employee ) s.get( Employee.class, ee.getId() );
				ee.setName( "Emmanuel2" );
				tx.commit();
				s.close();
	
				s = sf.openSession();
				tx = s.beginTransaction();
				er = ( Employer ) s.get( Employer.class, er.getId() );
				s.delete( er );
				ee = ( Employee ) s.get( Employee.class, ee.getId() );
				s.delete( ee );
				tx.commit();
				s.close();
			}
			catch ( Throwable t ) {
				allFine.set( false );
				t.printStackTrace();
			}
		}
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.Clock.directory_provider", "filesystem" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.show_sql", "false" );
		cfg.setProperty( "hibernate.format_sql", "false" );
	}

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
