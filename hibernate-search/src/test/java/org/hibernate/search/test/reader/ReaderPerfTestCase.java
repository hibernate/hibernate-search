/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.reader;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.Search;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public abstract class ReaderPerfTestCase extends SearchTestCase {

	private static final Logger log = LoggerFactory.make();

	protected void setUp() throws Exception {
		File sub = getBaseIndexDir();
		sub.mkdir();
		File[] files = sub.listFiles();
		for ( File file : files ) {
			if ( file.isDirectory() ) {
				FileHelper.delete( file );
			}
		}
		//super.setUp(); //we need a fresh session factory each time for index set up
		buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
	}

	@SuppressWarnings("unchecked")
	protected Class<?>[] getMappings() {
		return new Class[] {
				Detective.class,
				Suspect.class
		};
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if ( getSessions() != null ) {
			getSessions().close();
		}
		File sub = getBaseIndexDir();
		FileHelper.delete( sub );
	}

	public boolean insert = true;

	public void testConcurrency() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		for ( int index = 0; index < 5000; index++ ) {
			Detective detective = new Detective();
			detective.setName( "John Doe " + index );
			detective.setBadge( "123455" + index );
			detective.setPhysicalDescription( "Blond green eye etc etc" );
			s.persist( detective );
			Suspect suspect = new Suspect();
			suspect.setName( "Jane Doe " + index );
			suspect.setPhysicalDescription( "brunette, short, 30-ish" );
			if ( index % 20 == 0 ) {
				suspect.setSuspectCharge( "thief liar " );
			}
			else {
				suspect.setSuspectCharge(
						" It's 1875 in London. The police have captured career criminal Montmorency. In the process he has been grievously wounded and it is up to a young surgeon to treat his wounds. During his recovery Montmorency learns of the city's new sewer system and sees in it the perfect underground highway for his thievery.  Washington Post columnist John Kelly recommends this title for middle schoolers, especially to be read aloud."
				);
			}
			s.persist( suspect );
		}
		tx.commit();
		s.close();

		Thread.sleep( 1000 );

		int nThreads = 15;
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		Work work = new Work( getSessions() );
		ReverseWork reverseWork = new ReverseWork( getSessions() );
		long start = System.currentTimeMillis();
		int iteration = 100;
		log.info( "Starting worker threads." );
		for ( int i = 0; i < iteration; i++ ) {
			es.execute( work );
			es.execute( reverseWork );
		}
		while ( work.count < iteration - 1 ) {
			Thread.sleep( 20 );
		}
		log.debug( iteration + " iterations in " + nThreads + " threads: " + ( System.currentTimeMillis() - start ) );
	}

	protected class Work implements Runnable {
		private Random random = new Random();
		private SessionFactory sf;
		public volatile int count = 0;

		public Work(SessionFactory sf) {
			this.sf = sf;
		}

		public void run() {
			Session s = sf.openSession();
			Transaction tx = s.beginTransaction();
			QueryParser parser = new MultiFieldQueryParser( getTargetLuceneVersion(),
					new String[] { "name", "physicalDescription", "suspectCharge" },
					SearchTestCase.standardAnalyzer
			);
			FullTextQuery query = getQuery( "John Doe", parser, s );
			assertTrue( query.getResultSize() != 0 );

			query = getQuery( "green", parser, s );
			random.nextInt( query.getResultSize() - 15 );
			query.setFirstResult( random.nextInt( query.getResultSize() - 15 ) );
			query.setMaxResults( 10 );
			query.list();
			tx.commit();
			s.close();

			s = sf.openSession();
			tx = s.beginTransaction();

			query = getQuery( "John Doe", parser, s );
			assertTrue( query.getResultSize() != 0 );

			query = getQuery( "thief", parser, s );
			int firstResult = random.nextInt( query.getResultSize() - 15 );
			query.setFirstResult( firstResult );
			query.setMaxResults( 10 );
			List result = query.list();
			Object object = result.get( 0 );
			if ( insert && object instanceof Detective ) {
				Detective detective = ( Detective ) object;
				detective.setPhysicalDescription( detective.getPhysicalDescription() + " Eye" + firstResult );
			}
			else if ( insert && object instanceof Suspect ) {
				Suspect suspect = ( Suspect ) object;
				suspect.setPhysicalDescription( suspect.getPhysicalDescription() + " Eye" + firstResult );
			}
			tx.commit();
			s.close();
			count++;
		}

		private FullTextQuery getQuery(String queryString, QueryParser parser, Session s) {
			Query luceneQuery = null;
			try {
				luceneQuery = parser.parse( queryString );
			}
			catch ( ParseException e ) {
				e.printStackTrace();
			}
			return Search.getFullTextSession( s ).createFullTextQuery( luceneQuery );
		}
	}

	protected static class ReverseWork implements Runnable {
		private SessionFactory sf;
		private Random random = new Random();

		public ReverseWork(SessionFactory sf) {
			this.sf = sf;
		}

		public void run() {
			Session s = sf.openSession();
			Transaction tx = s.beginTransaction();
			QueryParser parser = new MultiFieldQueryParser( getTargetLuceneVersion(), 
					new String[] { "name", "physicalDescription", "suspectCharge" },
					SearchTestCase.standardAnalyzer
			);
			FullTextQuery query = getQuery( "John Doe", parser, s );
			assertTrue( query.getResultSize() != 0 );

			query = getQuery( "london", parser, s );
			random.nextInt( query.getResultSize() - 15 );
			query.setFirstResult( random.nextInt( query.getResultSize() - 15 ) );
			query.setMaxResults( 10 );
			query.list();
			tx.commit();
			s.close();

			s = sf.openSession();
			tx = s.beginTransaction();

			getQuery( "John Doe", parser, s );
			assertTrue( query.getResultSize() != 0 );

			query = getQuery( "green", parser, s );
			random.nextInt( query.getResultSize() - 15 );
			query.setFirstResult( random.nextInt( query.getResultSize() - 15 ) );
			query.setMaxResults( 10 );
			query.list();
			tx.commit();
			s.close();
		}

		private FullTextQuery getQuery(String queryString, QueryParser parser, Session s) {
			Query luceneQuery = null;
			try {
				luceneQuery = parser.parse( queryString );
			}
			catch ( ParseException e ) {
				e.printStackTrace();
			}
			return Search.getFullTextSession( s ).createFullTextQuery( luceneQuery );
		}
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

}
