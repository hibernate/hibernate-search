/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.test.jgroups.master.TShirt;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Moren
 * @author Sanne Grinovero
 */
public class JGroupsCommonTest extends StaticMasterSlaveSearchTestCase {

	public static final String TESTING_JGROUPS_CONFIGURATION_FILE = "testing-flush-loopback.xml";
	public static final Poller POLLER = Poller.milliseconds( 10_000, 100 );

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	@Test
	public void testJGroupsBackend() throws Exception {

		TShirt ts;
		TShirt ts2;
		FullTextSession masterSession;

		QueryParser parser = new QueryParser(
				"id",
				TestConstants.stopAnalyzer
		);

		//get slave session
		try ( Session slaveSession = getSlaveSession() ) {
			Transaction tx = slaveSession.beginTransaction();
			ts = new TShirt();
			ts.setLogo( "Boston" );
			ts.setSize( "XXL" );
			ts.setLength( 23.4d );
			ts2 = new TShirt();
			ts2.setLogo( "Mapple leaves" );
			ts2.setSize( "L" );
			ts2.setLength( 23.42d );
			slaveSession.persist( ts );
			slaveSession.persist( ts2 );
			tx.commit();

			masterSession = Search.getFullTextSession( openSession() ); //this is the master Session

			// since this is an async backend, we expect to see
			// the values in the index *eventually*.
			POLLER.pollAssertion( () -> {
				masterSession.getTransaction().begin();
				Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
				FullTextQuery query = masterSession.createFullTextQuery( luceneQuery );
				List result = query.list();
				masterSession.getTransaction().commit();

				Assert.assertEquals( "Lots of time waited and still the two documents are not indexed yet!",
						2, result.size() );
			} );
		}

		try ( Session slaveSession = getSlaveSession() ) {
			Transaction tx = slaveSession.beginTransaction();
			ts = (TShirt) slaveSession.get( TShirt.class, ts.getId() );
			ts.setLogo( "Peter pan" );
			tx.commit();

			POLLER.pollAssertion( () -> {
				Query luceneQuery = parser.parse( "logo:Peter pan" );
				masterSession.getTransaction().begin();
				FullTextQuery query = masterSession.createFullTextQuery( luceneQuery );
				List result = query.list();
				masterSession.getTransaction().commit();
				Assert.assertEquals( "Waited for long and still Peter Pan didn't fly in!", 1, result.size() );
			} );
		}

		try ( Session slaveSession = getSlaveSession() ) {
			Transaction tx = slaveSession.beginTransaction();
			slaveSession.delete( slaveSession.get( TShirt.class, ts.getId() ) );
			slaveSession.delete( slaveSession.get( TShirt.class, ts2.getId() ) );
			tx.commit();

			POLLER.pollAssertion( () -> {
				Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
				masterSession.getTransaction().begin();
				FullTextQuery query = masterSession.createFullTextQuery( luceneQuery );
				List result = query.list();
				masterSession.getTransaction().commit();
				Assert.assertEquals( "Waited for long and elements where still not deleted!", 0, result.size() );
			} );
		}
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		//master jgroups configuration
		super.configure( cfg );
		applyCommonJGroupsChannelConfiguration( cfg );
	}

	@Override
	protected void configureSlave(Map<String,Object> cfg) {
		//slave jgroups configuration
		super.configureSlave( cfg );
		applyCommonJGroupsChannelConfiguration( cfg );
	}

	/**
	 * Used to isolate the JGroups channel name from other potentially running tests
	 *
	 * @param cfg the configuration to isolate
	 */
	protected void applyCommonJGroupsChannelConfiguration(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default." + DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.put( DispatchMessageSender.CONFIGURATION_FILE, TESTING_JGROUPS_CONFIGURATION_FILE );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TShirt.class };
	}

}
