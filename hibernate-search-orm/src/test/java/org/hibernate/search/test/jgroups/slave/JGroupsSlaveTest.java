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

import org.jgroups.Channel;
import org.jgroups.JChannel;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.util.impl.XMLHelper;

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

		//need to sleep for the message consumption
		Thread.sleep( JGroupsCommonTest.NETWORK_TIMEOUT );

		assertEquals( 1, JGroupsReceiver.queues );
		assertEquals( 2, JGroupsReceiver.works );

		JGroupsReceiver.reset();
		s = openSession();
		tx = s.beginTransaction();
		ts = ( TShirt ) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep( JGroupsCommonTest.NETWORK_TIMEOUT );

		assertEquals( 1, JGroupsReceiver.queues );
		assertEquals( 1, JGroupsReceiver.works );

		JGroupsReceiver.reset();
		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( TShirt.class, ts.getId() ) );
		tx.commit();

		//Need to sleep for the message consumption
		Thread.sleep( JGroupsCommonTest.NETWORK_TIMEOUT );

		assertEquals( 1, JGroupsReceiver.queues );
		assertEquals( 1, JGroupsReceiver.works );
		s.close();
	}

	private void prepareJGroupsChannel() throws Exception {
		channel = new JChannel( XMLHelper.elementFromString( prepareXmlJGroupsConfiguration() ) );
		channel.connect( CHANNEL_NAME );
		channel.setReceiver( new JGroupsReceiver(getSearchFactoryImpl()) );
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
		cfg.setProperty( "hibernate.search.default." + JGroupsBackendQueueProcessor.JG_CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( "hibernate.search.default." + JGroupsBackendQueueProcessor.CONFIGURATION_XML, prepareXmlJGroupsConfiguration() );
	}

	private String prepareXmlJGroupsConfiguration() {
		return "<config>" +
				"<SHARED_LOOPBACK/>" +
				"<PING timeout=\"100\" num_initial_members=\"2\"/>" +
				"<MERGE2 max_interval=\"30000\" min_interval=\"10000\"/>" +
				"<FD_SOCK/>" +
				"<FD timeout=\"10000\" max_tries=\"5\"/>" +
				"<VERIFY_SUSPECT timeout=\"1500\"/>" +
				"<pbcast.NAKACK " +
				"            use_mcast_xmit=\"false\"" +
				"            retransmit_timeout=\"30,60,120,240,480\"" +
				"            discard_delivered_msgs=\"false\"/>" +
				"<UNICAST timeout=\"30,60,120,240,360\"/>" +
				"<pbcast.STABLE stability_delay=\"1000\" desired_avg_gossip=\"50000\"" +
				"            max_bytes=\"400000\"/>   " +
				"<pbcast.GMS print_local_addr=\"true\" join_timeout=\"200\"" +
				"            view_bundling=\"true\"/>" +
				"</config>";
	}
}
