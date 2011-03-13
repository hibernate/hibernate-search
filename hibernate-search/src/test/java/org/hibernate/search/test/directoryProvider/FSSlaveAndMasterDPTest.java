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
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.queryParser.QueryParser;
import org.slf4j.Logger;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * Test case for master/slave directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class FSSlaveAndMasterDPTest extends MultipleSFTestCase {

	private static final Logger log = LoggerFactory.make();

	static File root;

	static {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		root = new File( buildDir, "lucenedirs" );
		log.info( "Using {} as test directory.", root.getAbsolutePath() );
	}

	/**
	 * The lucene index directory which is shared between master and slave.
	 */
	final static String masterCopy = "/master/copy";

	/**
	 * The lucene index directory which is specific to the master node.
	 */
	final static String masterMain = "/master/main";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	final static String slave = "/slave";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	final static String slaveUnready = "/slaveUnready";

	/**
	 * Verifies that copies of the master get properly copied to the slaves.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testProperCopy() throws Exception {

		// assert that the salve index is empty
		FullTextSession fullTextSession = Search.getFullTextSession( getSlaveSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "id", SearchTestCase.stopAnalyzer );
		List result = fullTextSession.createFullTextQuery( parser.parse( "location:texas" ) ).list();
		assertEquals( "No copy yet, fresh index expected", 0, result.size() );
		tx.commit();
		fullTextSession.close();


		// create an entity on the master and persist it in order to index it
		Session session = getMasterSession();
		tx = session.beginTransaction();
		SnowStorm sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Dallas, TX, USA" );
		session.persist( sn );
		tx.commit();
		session.close();

		int waitPeriodMilli = 2010; // wait  a bit more than 2 refresh periods (one master / one slave)  -  2 * 1 * 1000 + 10
		Thread.sleep( waitPeriodMilli );

		// assert that the master hass indexed the snowstorm
		log.info( "Searching master" );
		fullTextSession = Search.getFullTextSession( getMasterSession() );
		tx = fullTextSession.beginTransaction();
		result = fullTextSession.createFullTextQuery( parser.parse( "location:dallas" ) ).list();
		assertEquals( "Original should get one", 1, result.size() );
		tx.commit();
		fullTextSession.close();

		// assert that index got copied to the salve as well
		log.info( "Searching slave" );
		fullTextSession = Search.getFullTextSession( getSlaveSession() );
		tx = fullTextSession.beginTransaction();
		result = fullTextSession.createFullTextQuery( parser.parse( "location:dallas" ) ).list();
		assertEquals( "First copy did not work out", 1, result.size() );
		tx.commit();
		fullTextSession.close();

		// add a new snowstorm to the master
		session = getMasterSession();
		tx = session.beginTransaction();
		sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Chennai, India" );
		session.persist( sn );
		tx.commit();
		session.close();

		Thread.sleep( waitPeriodMilli ); //wait a bit more than 2 refresh (one master / one slave)

		// assert that the new snowstorm made it into the slave
		log.info( "Searching slave" );
		fullTextSession = Search.getFullTextSession( getSlaveSession() );
		tx = fullTextSession.beginTransaction();
		result = fullTextSession.createFullTextQuery( parser.parse( "location:chennai" ) ).list();
		assertEquals( "Second copy did not work out", 1, result.size() );
		tx.commit();
		fullTextSession.close();

		session = getMasterSession();
		tx = session.beginTransaction();
		sn = new SnowStorm();
		sn.setDate( new Date() );
		sn.setLocation( "Melbourne, Australia" );
		session.persist( sn );
		tx.commit();
		session.close();

		Thread.sleep( waitPeriodMilli ); //wait a bit more than 2 refresh (one master / one slave)

		// once more - assert that the new snowstorm made it into the slave
		log.info( "Searching slave" );
		fullTextSession = Search.getFullTextSession( getSlaveSession() );
		tx = fullTextSession.beginTransaction();
		result = fullTextSession.createFullTextQuery( parser.parse( "location:melbourne" ) ).list();
		assertEquals( "Third copy did not work out", 1, result.size() );
		tx.commit();
		fullTextSession.close();
	}

	private Session getMasterSession() {
		return getSessionFactories()[0].openSession();
	}

	private Session getSlaveSession() {
		return getSessionFactories()[1].openSession();
	}
	
	static void prepareDirectories() {
		if ( root.exists() ) {
			FileHelper.delete( root );
		}

		if ( !root.mkdir() ) {
			throw new HibernateException( "Unable to setup test directories" );
		}

		File master = new File( root, masterMain );
		if ( !master.mkdirs() ) {
			throw new HibernateException( "Unable to setup master directory" );
		}

		master = new File( root, masterCopy );
		if ( !master.mkdirs() ) {
			throw new HibernateException( "Unable to setup master copy directory" );
		}

		File slaveFile = new File( root, slave );
		if ( !slaveFile.mkdirs() ) {
			throw new HibernateException( "Unable to setup slave directory" );
		}

		File slaveUnreadyFile = new File( root, slaveUnready );
		if ( !slaveUnreadyFile.mkdirs() ) {
			throw new HibernateException( "Unable to setup slave directory" );
		}
	}

	protected void setUp() throws Exception {
		prepareDirectories();
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		cleanupDirectories();
	}

	static void cleanupDirectories() {
		log.info( "Deleting test directory {} ", root.getAbsolutePath() );
		FileHelper.delete( root );
	}

	protected int getSFNbrs() {
		return 2;
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				SnowStorm.class
		};
	}

	public void testSourceNotReady() throws Exception {
		int retries = 1;
		Configuration cfg = new Configuration();
		//slave(s)
		cfg.setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy + "nooooot" );
		cfg.setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + slave );
		cfg.setProperty( "hibernate.search.default.refresh", "1" ); //every second
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
		cfg.setProperty(
				"hibernate.search.default.retry_marker_lookup", new Integer(retries).toString()
		);
		cfg.addAnnotatedClass( SnowStorm.class );
		long start = System.nanoTime();
		try {
			cfg.buildSessionFactory();
		}
		catch ( HibernateException e ) {
			assertTrue( "expected configuration failure", e.getCause() instanceof SearchException );
			final long elapsedTime = TimeUnit.NANOSECONDS.toSeconds( System.nanoTime() - start );
			assertTrue( "Should be around 10 seconds: " + elapsedTime, elapsedTime > retries*5 - 1 ); // -1 for safety
		}
	}

	protected void configure(Configuration[] cfg) {
		//master
		cfg[0].setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy );
		cfg[0].setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + masterMain );
		cfg[0].setProperty( "hibernate.search.default.refresh", "1" ); //every second
		cfg[0].setProperty(
				"hibernate.search.default.directory_provider", "filesystem-master"
		);

		//slave(s)
		cfg[1].setProperty( "hibernate.search.default.sourceBase", root.getAbsolutePath() + masterCopy );
		cfg[1].setProperty( "hibernate.search.default.indexBase", root.getAbsolutePath() + slave );
		cfg[1].setProperty( "hibernate.search.default.refresh", "1" ); //every second
		//keep the fqcn to make sure non short cut solutions still work
		cfg[1].setProperty(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
	}
}
