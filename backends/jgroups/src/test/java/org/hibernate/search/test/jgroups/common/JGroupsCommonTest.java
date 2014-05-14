/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.List;
import java.util.UUID;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.test.jgroups.master.TShirt;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Moren
 * @author Sanne Grinovero
 */
public class JGroupsCommonTest extends MultipleSessionsSearchTestCase {

	public static final String TESTING_JGROUPS_CONFIGURATION_FILE = "testing-flush-loopback.xml";
	public static final long NETWORK_WAIT_MILLISECONDS = 100;
	public static final int MAX_WAITS = 100;

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	@Test
	public void testJGroupsBackend() throws Exception {

		//get slave session
		Session slaveSession = getSlaveSession();
		Transaction tx = slaveSession.beginTransaction();
		TShirt ts = new TShirt();
		ts.setLogo( "Boston" );
		ts.setSize( "XXL" );
		ts.setLength( 23.4d );
		TShirt ts2 = new TShirt();
		ts2.setLogo( "Mapple leaves" );
		ts2.setSize( "L" );
		ts2.setLength( 23.42d );
		slaveSession.persist( ts );
		slaveSession.persist( ts2 );
		tx.commit();

		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"id",
				TestConstants.stopAnalyzer
		);
		FullTextSession masterSession = Search.getFullTextSession( openSession() ); //this is the master Session

		// since this is an async backend, we expect to see
		// the values in the index *eventually*.
		boolean failed = true;
		for ( int i = 0; i < MAX_WAITS; i++ ) {
			Thread.sleep( NETWORK_WAIT_MILLISECONDS );

			masterSession.getTransaction().begin();
			Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
			org.hibernate.Query query = masterSession.createFullTextQuery( luceneQuery );
			List result = query.list();
			masterSession.getTransaction().commit();

			if ( result.size() == 2 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}

		if ( failed ) {
			Assert.fail( "Lots of time waited and still the two documents are not indexed yet!" );
		}

		slaveSession = getSlaveSession();
		tx = slaveSession.beginTransaction();
		ts = (TShirt) slaveSession.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		failed = true;
		for ( int i = 0; i < MAX_WAITS; i++ ) {
			//need to sleep for the message consumption
			Thread.sleep( NETWORK_WAIT_MILLISECONDS );

			Query luceneQuery = parser.parse( "logo:Peter pan" );
			masterSession.getTransaction().begin();
			org.hibernate.Query query = masterSession.createFullTextQuery( luceneQuery );
			List result = query.list();
			masterSession.getTransaction().commit();
			if ( result.size() == 1 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}

		if ( failed ) {
			Assert.fail( "Waited for long and still Peter Pan didn't fly in!" );
		}

		slaveSession = getSlaveSession();
		tx = slaveSession.beginTransaction();
		slaveSession.delete( slaveSession.get( TShirt.class, ts.getId() ) );
		slaveSession.delete( slaveSession.get( TShirt.class, ts2.getId() ) );
		tx.commit();

		failed = true;
		for ( int i = 0; i < MAX_WAITS; i++ ) {
			//need to sleep for the message consumption
			Thread.sleep( NETWORK_WAIT_MILLISECONDS );

			Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
			masterSession.getTransaction().begin();
			org.hibernate.Query query = masterSession.createFullTextQuery( luceneQuery );
			List result = query.list();
			masterSession.getTransaction().commit();
			if ( result.size() == 0 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}

		if ( failed ) {
			Assert.fail( "Waited for long and elements where still not deleted!" );
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		//master jgroups configuration
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		applyJGroupsChannelConfiguration( cfg );
	}

	@Override
	protected void commonConfigure(Configuration cfg) {
		//slave jgroups configuration
		super.commonConfigure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.setProperty( "hibernate.search.default.retry_initialize_period", "1" );
		applyJGroupsChannelConfiguration( cfg );
	}

	/**
	 * Used to isolate the JGroups channel name from other potentially running tests
	 *
	 * @param cfg the configuration to isolate
	 */
	protected void applyJGroupsChannelConfiguration(Configuration cfg) {
		cfg.setProperty( "hibernate.search.default." + DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( DispatchMessageSender.CONFIGURATION_FILE, TESTING_JGROUPS_CONFIGURATION_FILE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	@Override
	protected Class<?>[] getCommonAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

}
