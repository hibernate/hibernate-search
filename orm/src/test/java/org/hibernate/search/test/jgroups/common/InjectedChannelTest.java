/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.jgroups.common;

import junit.framework.Assert;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.backend.impl.jgroups.MessageSender;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.jgroups.JChannel;
import org.junit.Test;

/**
 * A JGroups JChannel instance can be injected in the configuration
 * directly: verify the passed instance is the one being used.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class InjectedChannelTest extends JGroupsCommonTest {

	private JChannel masterChannel;
	private JChannel slaveChannel;

	@Test
	public void testInjectionHappened() throws Exception {
		MutableSearchFactory searchFactory = (MutableSearchFactory) getSearchFactory();
		MessageSender sender = searchFactory.getServiceManager().requestService( JGroupsChannelProvider.class, null );
		Assert.assertTrue( masterChannel.getAddress().equals( sender.getAddress() ) );
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
	protected void configure(Configuration cfg) {
		//master jgroups configuration
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.getProperties().put( JGroupsChannelProvider.CHANNEL_INJECT, masterChannel );
	}

	@Override
	protected void commonConfigure(Configuration cfg) {
		//slave jgroups configuration
		super.commonConfigure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.getProperties().put( JGroupsChannelProvider.CHANNEL_INJECT, slaveChannel );
	}

	private static JChannel createChannel() throws Exception {
		JChannel channel = new JChannel( ConfigurationParseHelper.locateConfig( JGroupsCommonTest.TESTING_JGROUPS_CONFIGURATION_FILE ) );
		channel.connect( "JGroupsCommonTest" + JGroupsCommonTest.CHANNEL_NAME );
		return channel;
	}

}
