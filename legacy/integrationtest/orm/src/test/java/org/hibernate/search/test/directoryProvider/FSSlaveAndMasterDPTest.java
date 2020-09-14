/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test case for master/slave directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // Directories are specific to the Lucene backend
public class FSSlaveAndMasterDPTest extends MultipleSFTestCase {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * The lucene index directory which is shared between master and slave.
	 */
	static final String masterCopy = "master/copy";

	/**
	 * The lucene index directory which is specific to the master node.
	 */
	static final String masterMain = "master/main";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	static final String slave = "slave";

	/**
	 * The lucene index directory which is specific to the slave node.
	 */
	static final String slaveUnready = "slaveUnready";

	private Path root;

	/**
	 * Verifies that copies of the master get properly copied to the slaves.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testProperCopy() throws Exception {

		// assert that the slave index is empty
		FullTextSession fullTextSession = Search.getFullTextSession( getSlaveSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );
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

		// assert that the master has indexed the snowstorm
		log.debug( "Searching master" );
		fullTextSession = Search.getFullTextSession( getMasterSession() );
		tx = fullTextSession.beginTransaction();
		result = fullTextSession.createFullTextQuery( parser.parse( "location:dallas" ) ).list();
		assertEquals( "Original should get one", 1, result.size() );
		tx.commit();
		fullTextSession.close();

		// assert that index got copied to the salve as well
		log.debug( "Searching slave" );
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
		log.debug( "Searching slave" );
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
		log.debug( "Searching slave" );
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

	static Path prepareDirectories(String testId) throws IOException {

		Path superRootPath = TestConstants.getIndexDirectory( TestConstants.getTempTestDataDir() );
		Path root = superRootPath.resolve( testId );

		Files.deleteIfExists( root );
		Files.createDirectories( root );

		Files.createDirectories( root.resolve( masterMain ) );
		Files.createDirectories( root.resolve( masterCopy ) );
		Files.createDirectories( root.resolve( slave ) );
		Files.createDirectories( root.resolve( slaveUnready ) );

		return root;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		this.root = prepareDirectories( getClass().getSimpleName() );
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		cleanupDirectories( root );
	}

	static void cleanupDirectories( Path root ) throws IOException {
		log.debugf( "Deleting test directory %s ", root.toAbsolutePath() );
		FileHelper.delete( root );
	}

	@Override
	protected int getSFNbrs() {
		return 2;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				SnowStorm.class
		};
	}

	@Test
	public void testSourceNotReady() throws Exception {
		int retries = 1;
		Configuration cfg = new Configuration();
		//slave(s)
		cfg.setProperty( "hibernate.search.default.sourceBase", root.toAbsolutePath() + masterCopy + "nooooot" );
		cfg.setProperty( "hibernate.search.default.indexBase", root.toAbsolutePath() + slave );
		cfg.setProperty( "hibernate.search.default.refresh", "1" ); //every second
		cfg.setProperty( "hibernate.search.lucene_version", "LUCENE_CURRENT" );
		cfg.setProperty(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
		cfg.setProperty(
				"hibernate.search.default.retry_marker_lookup", String.valueOf( retries )
		);
		cfg.addAnnotatedClass( SnowStorm.class );
		long start = System.nanoTime();
		try {
			cfg.buildSessionFactory();
		}
		catch (SearchException e) {
			final long elapsedTime = TimeUnit.NANOSECONDS.toSeconds( System.nanoTime() - start );
			assertTrue( "Should be around 10 seconds: " + elapsedTime, elapsedTime > retries * 5 - 1 ); // -1 for safety
		}
	}

	@Override
	protected void configure(Configuration[] cfg) {
		//master
		cfg[0].setProperty( "hibernate.search.default.sourceBase", root.toAbsolutePath() + masterCopy );
		cfg[0].setProperty( "hibernate.search.default.indexBase", root.toAbsolutePath() + masterMain );
		cfg[0].setProperty( "hibernate.search.default.refresh", "1" ); //every second
		cfg[0].setProperty( "hibernate.search.lucene_version", "LUCENE_CURRENT" );
		cfg[0].setProperty(
				"hibernate.search.default.directory_provider", "filesystem-master"
		);

		//slave(s)
		cfg[1].setProperty( "hibernate.search.default.sourceBase", root.toAbsolutePath() + masterCopy );
		cfg[1].setProperty( "hibernate.search.default.indexBase", root.toAbsolutePath() + slave );
		cfg[1].setProperty( "hibernate.search.default.refresh", "1" ); //every second
		cfg[1].setProperty( "hibernate.search.lucene_version", "LUCENE_CURRENT" );
		//keep the fqcn to make sure non short cut solutions still work
		cfg[1].setProperty(
				"hibernate.search.default.directory_provider", "filesystem-slave"
		);
	}
}
