/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.Map;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.MessageSenderService;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.jgroups.JChannel;
import org.junit.Test;
import org.junit.Assert;

/**
 * A JGroups JChannel instance can be injected in the configuration
 * directly: verify the passed instance is the one being used.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class InjectedChannelTest extends JGroupsCommonTest {

	private JChannel masterChannel;
	private JChannel slaveChannel;

	@Test
	public void testInjectionHappened() throws Exception {
		SearchIntegrator searchFactory = getSearchFactory().unwrap( SearchIntegrator.class );
		MessageSenderService sender = searchFactory.getServiceManager().requestService( MessageSenderService.class );
		Assert.assertTrue( masterChannel.getAddress().equals( sender.getAddress() ) );
		searchFactory.getServiceManager().releaseService( MessageSenderService.class );
	}

	@Override
	public void setUp() throws Exception {
		masterChannel = createChannel();
		slaveChannel = createChannel();
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		try {
			super.tearDown();
		}
		finally {
			if ( masterChannel != null ) {
				masterChannel.close();
			}
			if ( slaveChannel != null ) {
				slaveChannel.close();
			}
		}
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		//master jgroups configuration
		super.configure( cfg );
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.put( DispatchMessageSender.CHANNEL_INJECT, masterChannel );
	}

	@Override
	protected void configureSlave(Map<String,Object> cfg) {
		//slave jgroups configuration
		super.configureSlave( cfg );
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.put( DispatchMessageSender.CHANNEL_INJECT, slaveChannel );
	}

	private static JChannel createChannel() throws Exception {
		JChannel channel = new JChannel( ConfigurationParseHelper.locateConfig( JGroupsCommonTest.TESTING_JGROUPS_CONFIGURATION_FILE ) );
		channel.connect( "JGroupsCommonTest" + JGroupsCommonTest.CHANNEL_NAME );
		return channel;
	}

}
