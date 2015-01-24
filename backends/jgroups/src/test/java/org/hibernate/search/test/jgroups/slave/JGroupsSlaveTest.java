/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.slave;

import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.jgroups.impl.MessageListenerToRequestHandlerAdapter;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsSlaveTest extends SearchTestBase {

	private Channel channel;

	/** makes sure that different tests don't join **/
	private final String CHANNEL_NAME = UUID.randomUUID().toString();

	@Test
	public void testMessageSend() throws Exception {

		JGroupsReceiver.reset();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		TShirt ts = new TShirt();
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

		boolean failed = true;
		for ( int i = 0; i < JGroupsCommonTest.MAX_WAITS; i++ ) {
			Thread.sleep( JGroupsCommonTest.NETWORK_WAIT_MILLISECONDS );
			if ( JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 2 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}
		if ( failed ) {
			Assert.fail( "Message not received after waiting for long!" );
		}

		JGroupsReceiver.reset();
		s = openSession();
		tx = s.beginTransaction();
		ts = (TShirt) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		failed = true;
		for ( int i = 0; i < JGroupsCommonTest.MAX_WAITS; i++ ) {
			Thread.sleep( JGroupsCommonTest.NETWORK_WAIT_MILLISECONDS );
			if ( JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 1 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}
		if ( failed ) {
			Assert.fail( "Message not received after waiting for long!" );
		}

		JGroupsReceiver.reset();
		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( TShirt.class, ts.getId() ) );
		tx.commit();

		failed = true;
		for ( int i = 0; i < JGroupsCommonTest.MAX_WAITS; i++ ) {
			Thread.sleep( JGroupsCommonTest.NETWORK_WAIT_MILLISECONDS );
			if ( JGroupsReceiver.queues == 1 && JGroupsReceiver.works == 1 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}
		if ( failed ) {
			Assert.fail( "Message not received after waiting for long!" );
		}

		s.close();
	}

	private void prepareJGroupsChannel() throws Exception {
		channel = new JChannel( ConfigurationParseHelper.locateConfig( "testing-flush-loopback.xml" ) );
		channel.connect( CHANNEL_NAME );
		JGroupsReceiver listener = new JGroupsReceiver(getExtendedSearchIntegrator());
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.setProperty( "hibernate.search.default." + JGroupsBackendQueueProcessor.BLOCK_WAITING_ACK, "false" );
		cfg.setProperty( "hibernate.search.default." + JGroupsBackendQueueProcessor.MESSAGE_TIMEOUT_MS, "100" );
		cfg.setProperty( DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( DispatchMessageSender.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

}
