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
import org.hibernate.search.util.impl.XMLHelper;
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
	public static final String CONFIGURATION_STRING = JGROUPS_PREFIX + "configurationString";
	public static final String CONFIGURATION_XML = JGROUPS_PREFIX + "configurationXml";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	public static final String JG_CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";
	public static final String JG_CHANNEL_INJECT = JGROUPS_PREFIX + "providedChannel";
	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";

	protected String clusterName = "HSearchCluster";

	private Channel channel;
	private MessageListener masterListener;
	private boolean channelIsManaged = true;

	@Override
	public void start(Properties props, BuildContext context) {
		this.clusterName = props.getProperty( JGroupsChannelProvider.JG_CLUSTER_NAME, "HSearchCluster" );
		prepareJGroupsChannel( props, context );
	}

	@Override
	public Channel getService() {
		return channel;
	}

	@Override
	public void stop() {
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
		log.jGroupsStartingChannel();
		try {
			buildChannel( props );
			GlobalMasterSelector masterNodeSelector = context.requestService( MasterSelectorServiceProvider.class );
			masterListener = new MessageListener( context, masterNodeSelector );
			channel.setReceiver( masterListener );
			if ( channelIsManaged ) {
				channel.connect( clusterName );
			}
			masterNodeSelector.setLocalAddress( channel.getAddress() );
		}
		catch ( Exception e ) {
			throw log.unabletoConnectToJGroupsCluster( clusterName, e );
		}
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
			if ( props.containsKey( JGroupsChannelProvider.JG_CHANNEL_INJECT ) ) {
				Object channelObject = props.get( JGroupsChannelProvider.JG_CHANNEL_INJECT );
				try {
					channel = (org.jgroups.JChannel) channelObject;
					channelIsManaged = false;
				}
				catch ( ClassCastException e ) {
					throw log.jGroupsChannelInjectionError( e );
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

			if ( props.containsKey( JGroupsChannelProvider.CONFIGURATION_XML ) ) {
				cfg = props.getProperty( JGroupsChannelProvider.CONFIGURATION_XML );
				try {
					channel = new JChannel( XMLHelper.elementFromString( cfg ) );
				}
				catch ( Exception e ) {
					throw log.jGroupsChannelCreationUsingXmlError( cfg, e );
				}
			}

			if ( props.containsKey( JGroupsChannelProvider.CONFIGURATION_STRING ) ) {
				cfg = props.getProperty( JGroupsChannelProvider.CONFIGURATION_STRING );
				try {
					channel = new JChannel( cfg );
				}
				catch ( Exception e ) {
					throw log.jGroupsChannelCreationFromStringError( cfg, e );
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
