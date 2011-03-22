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

import org.jgroups.Channel;
import org.jgroups.JChannel;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessorFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.util.XMLHelper;

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
 */
public class JGroupsSlaveTest extends SearchTestCase {

	public static final String CHANNEL_NAME = "HSearchCluster";

	private Channel channel;

	public void testMessageSend() throws Exception {

		JGroupsReceiver.reset();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		TShirt ts = new TShirt();
		ts.setLogo( "Boston" );
		ts.setSize( "XXL" );
		TShirt ts2 = new TShirt();
		ts2.setLogo( "Mapple leaves" );
		ts2.setSize( "L" );
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
		assertEquals( 2, JGroupsReceiver.works );

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
		channel.setReceiver( new JGroupsReceiver() );
	}

	protected void setUp() throws Exception {
		super.setUp();
		prepareJGroupsChannel();
	}

	protected void tearDown() throws Exception {
		channel.close();
		super.tearDown();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.setProperty( JGroupsBackendQueueProcessorFactory.CONFIGURATION_XML, prepareXmlJGroupsConfiguration() );
	}

	private String prepareXmlJGroupsConfiguration() {
		return "<config>" +
				"<UDP" +
				"     mcast_addr=\"${jgroups.udp.mcast_addr:228.10.10.10}\"" +
				"     mcast_port=\"${jgroups.udp.mcast_port:45588}\"" +
				"     tos=\"8\"" +
				"     ucast_recv_buf_size=\"20000000\"" +
				"     ucast_send_buf_size=\"640000\"" +
				"     mcast_recv_buf_size=\"25000000\"" +
				"     mcast_send_buf_size=\"640000\"" +
				"     loopback=\"false\"\n" +
				"     discard_incompatible_packets=\"true\"" +
				"     max_bundle_size=\"64000\"" +
				"     max_bundle_timeout=\"30\"" +
				"     use_incoming_packet_handler=\"true\"" +
				"     ip_ttl=\"${jgroups.udp.ip_ttl:2}\"" +
				"     enable_bundling=\"true\"" +
				"     enable_diagnostics=\"true\"" +
				"     use_concurrent_stack=\"true\"" +
				"     thread_naming_pattern=\"pl\"" +
				"     thread_pool.enabled=\"true\"" +
				"     thread_pool.min_threads=\"1\"" +
				"     thread_pool.max_threads=\"5\"" +
				"     thread_pool.keep_alive_time=\"500\"" +
				"     thread_pool.queue_enabled=\"false\"" +
				"     thread_pool.queue_max_size=\"100\"" +
				"     thread_pool.rejection_policy=\"Run\"" +
				"     oob_thread_pool.enabled=\"true\"" +
				"     oob_thread_pool.min_threads=\"1\"" +
				"     oob_thread_pool.max_threads=\"8\"" +
				"     oob_thread_pool.keep_alive_time=\"500\"" +
				"     oob_thread_pool.queue_enabled=\"false\"" +
				"     oob_thread_pool.queue_max_size=\"100\"" +
				"     oob_thread_pool.rejection_policy=\"Run\"/>" +
				"<PING timeout=\"100\" num_initial_members=\"2\"/>" +
				"<MERGE2 max_interval=\"30000\" min_interval=\"10000\"/>" +
				"<FD_SOCK/>" +
				"<FD timeout=\"10000\" max_tries=\"5\" shun=\"true\"/>" +
				"<VERIFY_SUSPECT timeout=\"1500\"/>" +
				"<pbcast.NAKACK " +
				"            use_mcast_xmit=\"false\" gc_lag=\"0\"" +
				"            retransmit_timeout=\"30,60,120,240,480\"" +
				"            discard_delivered_msgs=\"false\"/>" +
				"<UNICAST timeout=\"30,60,120,240,360\"/>" +
				"<pbcast.STABLE stability_delay=\"1000\" desired_avg_gossip=\"50000\"" +
				"            max_bytes=\"400000\"/>   " +
				"<pbcast.GMS print_local_addr=\"true\" join_timeout=\"200\"" +
				"            shun=\"false\"" +
				"            view_bundling=\"true\"/>" +
				"<FC max_credits=\"20000000\" min_threshold=\"0.10\"/>" +
				"<FRAG2 frag_size=\"60000\"/>" +
				"<pbcast.STREAMING_STATE_TRANSFER />" +
				"<pbcast.FLUSH timeout=\"0\"/>" +
				"</config>";
	}
}
