/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.slave;

import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.JGroupsBackend;
import org.hibernate.search.backend.jgroups.impl.MessageListenerToRequestHandlerAdapter;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.blocks.MessageDispatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the Slave node in a JGroups cluster can properly send messages to the channel.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class JGroupsSlaveTest extends SearchTestBase {

	private static final Poller POLLER = JGroupsCommonTest.POLLER;

	private Channel channel;

	/** makes sure that different tests don't join **/
	private final String CHANNEL_NAME = UUID.randomUUID().toString();

	@Test
	public void testMessageSend() throws Exception {

		TShirt ts;
		JGroupsReceiver.reset();

		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			ts = new TShirt();
			ts.setLogo( "Boston" );
			ts.setSize( "XXL" );
			ts.setLength( 23.3d );
			TShirt ts2 = new TShirt();
			ts2.setLogo( "Mapple leaves" );
			ts2.setSize( "L" );
			ts2.setLength( 23.32d );
			s.persist( ts );
			s.persist( ts2 );
			tx.commit();

			POLLER.pollAssertion( () -> {
				Assert.assertTrue( "Message not received after waiting for long!",
						JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 2 );
			} );
		}

		JGroupsReceiver.reset();

		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			ts = (TShirt) s.get( TShirt.class, ts.getId() );
			ts.setLogo( "Peter pan" );
			tx.commit();

			POLLER.pollAssertion( () -> {
				Assert.assertTrue( "Message not received after waiting for long!",
						JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 1 );
			} );
		}

		JGroupsReceiver.reset();

		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			s.delete( s.get( TShirt.class, ts.getId() ) );
			tx.commit();

			POLLER.pollAssertion( () -> {
				Assert.assertTrue( "Message not received after waiting for long!",
						JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 1 );
			} );
		}
	}

	private void prepareJGroupsChannel() throws Exception {
		channel = new JChannel( ConfigurationParseHelper.locateConfig( "testing-flush-loopback.xml" ) );
		channel.connect( CHANNEL_NAME );
		JGroupsReceiver listener = new JGroupsReceiver( getExtendedSearchIntegrator() );
		MessageListenerToRequestHandlerAdapter adapter = new MessageListenerToRequestHandlerAdapter( listener );
		MessageDispatcher standardDispatcher = new MessageDispatcher( channel, listener, listener, adapter );
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		prepareJGroupsChannel();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		channel.close();
		super.tearDown();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.put( "hibernate.search.default." + JGroupsBackend.BLOCK_WAITING_ACK, "false" );
		cfg.put( "hibernate.search.default." + JGroupsBackend.MESSAGE_TIMEOUT_MS, "100" );
		cfg.put( DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.put( DispatchMessageSender.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

}
