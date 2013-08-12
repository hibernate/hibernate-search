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
package org.hibernate.search.test.performance.optimizer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class OptimizerPerformanceTest extends SearchTestCaseJUnit4 {
	@Override
	@Before
	public void setUp() throws Exception {
		forceConfigurationRebuild();
		File sub = getBaseIndexDir();
		FileHelper.delete( sub );
		sub.mkdirs();
		File[] files = sub.listFiles();
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
		File sub = getBaseIndexDir();
		FileHelper.delete( sub );
	}

	@Test
	public void testConcurrency() throws Exception {
		int nThreads = 15;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		Work work = new Work( getSessionFactory() );
		ReverseWork reverseWork = new ReverseWork( getSessionFactory() );
		long start = System.nanoTime();
		int iteration = 100;
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}
		while ( work.count < iteration - 1 ) {
			Thread.sleep( 20 );
		}
		System.out.println(
				iteration + " iterations (8 tx per iteration) in " + nThreads + " threads: "
						+ TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start )
		);
	}

	protected static class Work implements Runnable {
		private final SessionFactory sf;
		public volatile int count = 0;

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
				FullTextSession fts = new FullTextSessionImpl( s );
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
				count++;
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
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
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
