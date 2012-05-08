/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.backend.impl.jgroups;

import java.net.URL;
import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.Channel;
import org.jgroups.JChannel;

/**
 * Service to initialize a JGroups Channel. This needs to be centralized to
 * allow sharing of channels across different index managers.
 * 
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class JGroupsChannelProvider implements ServiceProvider<Channel> {

	private static final Log log = LoggerFactory.make();

	public static final String JGROUPS_PREFIX = "hibernate.search.services.jgroups.";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	public static final String CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";
	public static final String CHANNEL_INJECT = JGROUPS_PREFIX + "providedChannel";

	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";
	private static final String DEFAULT_CLUSTER_NAME = "Hibernate Search Cluster";

	protected String clusterName;

	private Channel channel;
	private JGroupsMasterMessageListener masterListener;
	private boolean channelIsManaged = true;
	private BuildContext context;

	@Override
	public void start(Properties props, BuildContext context) {
		this.clusterName = props.getProperty( JGroupsChannelProvider.CLUSTER_NAME, DEFAULT_CLUSTER_NAME );
		prepareJGroupsChannel( props, context );
	}

	@Override
	public Channel getService() {
		return channel;
	}

	@Override
	public void stop() {
		context.releaseService( MasterSelectorServiceProvider.class );
		context = null;
		try {
			if ( channelIsManaged && channel != null && channel.isOpen() ) {
				log.jGroupsDisconnectingAndClosingChannel();
				channel.disconnect();
				channel.close();
			}
		}
		catch ( Exception toLog ) {
			log.jGroupsClosingChannelError( toLog );
			channel = null;
		}
	}

	private void prepareJGroupsChannel(Properties props, BuildContext context) {
		this.context = context;
		log.jGroupsStartingChannel();
		buildChannel( props );
		NodeSelectorStrategyHolder masterNodeSelector = context.requestService( MasterSelectorServiceProvider.class );
		masterListener = new JGroupsMasterMessageListener( context, masterNodeSelector );
		channel.setReceiver( masterListener );
		if ( channelIsManaged ) {
			try {
				channel.connect( clusterName );
			}
			catch ( Exception e ) {
				throw log.unableConnectingToJGroupsCluster( clusterName, e );
			}
		}
		masterNodeSelector.setLocalAddress( channel.getAddress() );
		log.jGroupsConnectedToCluster( clusterName, channel.getAddress() );

		if ( !channel.flushSupported() ) {
			log.jGroupsFlushNotPresentInStack();
		}
	}

	/**
	 * Reads configuration and builds channel with its base.
	 * In order of preference - we first look for an external JGroups file, then a set of XML properties, and
	 * finally the legacy JGroups String properties.
	 * 
	 * @param props configuration file
	 */
	private void buildChannel(Properties props) {
		String cfg;
		if ( props != null ) {
			if ( props.containsKey( JGroupsChannelProvider.CHANNEL_INJECT ) ) {
				Object channelObject = props.get( JGroupsChannelProvider.CHANNEL_INJECT );
				try {
					channel = (org.jgroups.JChannel) channelObject;
					channelIsManaged = false;
				}
				catch ( ClassCastException e ) {
					throw log.jGroupsChannelInjectionError( e, channelObject.getClass() );
				}
			}

			if ( props.containsKey( JGroupsChannelProvider.CONFIGURATION_FILE ) ) {
				cfg = props.getProperty( JGroupsChannelProvider.CONFIGURATION_FILE );
				try {
					channel = new JChannel( ConfigurationParseHelper.locateConfig( cfg ) );
				}
				catch ( Exception e ) {
					throw log.jGroupsChannelCreationUsingFileError( cfg, e );
				}
			}
		}

		if ( channel == null ) {
			log.jGroupsConfigurationNotFoundInProperties( props );
			try {
				URL fileUrl = ConfigurationParseHelper.locateConfig( JGroupsChannelProvider.DEFAULT_JGROUPS_CONFIGURATION_FILE );
				if ( fileUrl != null ) {
					channel = new JChannel( fileUrl );
				}
				else {
					log.jGroupsDefaultConfigurationFileNotFound();
					channel = new JChannel();
				}
			}
			catch ( Exception e ) {
				throw log.unableToStartJGroupsChannel( e );
			}
		}
	}

}
