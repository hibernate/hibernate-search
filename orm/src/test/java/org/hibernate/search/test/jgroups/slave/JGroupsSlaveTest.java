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
package org.hibernate.search.test.jgroups.slave;

import java.util.UUID;

import junit.framework.Assert;

import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.blocks.MessageDispatcher;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.backend.impl.jgroups.MessageListenerToRequestHandlerAdapter;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Tests that the Slave node in a JGroups cluster can properly send messages to the channel.
 * <p/>
 * In case of running test outside Hibernate Search Maven configuration set following VM configuration:
 * <br><br>
 * <code>
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 * </code>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsSlaveTest extends SearchTestCase {

	private Channel channel;

	/** makes sure that different tests don't join **/
	private final String CHANNEL_NAME = UUID.randomUUID().toString();

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
		JGroupsReceiver listener = new JGroupsReceiver(getSearchFactoryImpl());
		MessageListenerToRequestHandlerAdapter adapter = new MessageListenerToRequestHandlerAdapter( listener );
		MessageDispatcher standardDispatcher = new MessageDispatcher( channel, listener, listener, adapter );
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		prepareJGroupsChannel();
	}

	@Override
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
		cfg.setProperty( JGroupsChannelProvider.CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( JGroupsChannelProvider.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

}
