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

import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.JChannel;
import org.jgroups.MessageListener;
import org.jgroups.UpHandler;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.mux.MuxMessageDispatcher;
import org.jgroups.blocks.mux.Muxer;

/**
 * Service to initialize a JGroups Channel. This needs to be centralized to
 * allow sharing of channels across different index managers.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Ales Justin
 */
public class JGroupsChannelProvider implements ServiceProvider<MessageSender> {

	private static final Log log = LoggerFactory.make();

	public static final String JGROUPS_PREFIX = "hibernate.search.services.jgroups.";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	public static final String CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";
	public static final String CHANNEL_INJECT = JGROUPS_PREFIX + "providedChannel";
	public static final String CLASSLOADER = JGROUPS_PREFIX + "classloader";
	public static final String MUX_ID = JGROUPS_PREFIX + "mux_id";

	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";
	private static final String DEFAULT_CLUSTER_NAME = "Hibernate Search Cluster";

	private ChannelContainer channelContainer;
	private MessageSender sender;
	private ServiceManager serviceManager;

	@Override
	public void start(Properties props, BuildContext context) {
		log.jGroupsStartingChannelProvider();
		serviceManager = context.getServiceManager();

		channelContainer = buildChannel( props );
		channelContainer.start();

		NodeSelectorStrategyHolder masterNodeSelector = serviceManager.requestService( MasterSelectorServiceProvider.class, context );
		JGroupsMasterMessageListener listener = new JGroupsMasterMessageListener( context, masterNodeSelector );

		JChannel channel = channelContainer.getChannel();

		UpHandler handler = channel.getUpHandler();
		if ( handler instanceof Muxer ) {
			Short muxId = (Short) props.get( MUX_ID );
			if ( muxId == null ) {
				throw log.missingJGroupsMuxId();
			}
			@SuppressWarnings("unchecked")
			Muxer<UpHandler> muxer = (Muxer<UpHandler>) handler;
			if ( muxer.get( muxId ) != null ) {
				throw log.jGroupsMuxIdAlreadyTaken( muxId );
			}

			ClassLoader cl = (ClassLoader) props.get( CLASSLOADER );
			MessageListener wrapper = ( cl != null ) ? new ClassloaderMessageListener( listener, cl ) : listener;
			MessageListenerToRequestHandlerAdapter adapter = new MessageListenerToRequestHandlerAdapter( wrapper );
			MessageDispatcher dispatcher = new MuxMessageDispatcher( muxId, channel, wrapper, listener, adapter );
			sender = new DispatcherMessageSender( dispatcher );
		}
		else {
			MessageListenerToRequestHandlerAdapter adapter = new MessageListenerToRequestHandlerAdapter( listener );
			MessageDispatcher standardDispatcher = new MessageDispatcher( channel, listener, listener, adapter );
			sender = new DispatcherMessageSender( standardDispatcher );
		}

		masterNodeSelector.setLocalAddress( channel.getAddress() );

		if ( !channel.flushSupported() ) {
			log.jGroupsFlushNotPresentInStack();
		}
		if ( log.isDebugEnabled() ) {
			log.jgroupsFullConfiguration( channel.getProtocolStack().printProtocolSpecAsXML() );
		}
	}

	@Override
	public MessageSender getService() {
		return sender;
	}

	@Override
	public void stop() {
		serviceManager.releaseService( MasterSelectorServiceProvider.class );
		serviceManager = null;
		try {
			if ( sender != null ) {
				sender.stop();
				sender = null;
			}
			if ( channelContainer != null ) {
				channelContainer.close();
				channelContainer = null;
			}
		}
		catch (Exception toLog) {
			log.jGroupsClosingChannelError( toLog );
		}
	}

	/**
	 * Reads configuration and builds channel with its base.
	 * In order of preference - we first look for an external JGroups file, then a set of XML properties, and
	 * finally the legacy JGroups String properties.
	 *
	 * @param props configuration file
	 * @return the ChannelContainer to manage the JGroups JChannel
	 */
	private static ChannelContainer buildChannel(Properties props) {
		final String clusterName = ConfigurationParseHelper.getString(
				props, JGroupsChannelProvider.CLUSTER_NAME, DEFAULT_CLUSTER_NAME );
		if ( props != null ) {
			final Object channelObject = props.get( JGroupsChannelProvider.CHANNEL_INJECT );
			if ( channelObject != null ) {
				try {
					return new InjectedChannelContainer( (org.jgroups.JChannel) channelObject );
				}
				catch (ClassCastException e) {
					throw log.jGroupsChannelInjectionError( e, channelObject.getClass() );
				}
			}

			final String cfg = props.getProperty( JGroupsChannelProvider.CONFIGURATION_FILE );
			if ( cfg != null ) {
				try {
					log.startingJGroupsChannel( cfg );
					return new ManagedChannelContainer( new JChannel( ConfigurationParseHelper.locateConfig( cfg ) ), clusterName );
				}
				catch (Exception e) {
					throw log.jGroupsChannelCreationUsingFileError( cfg, e );
				}
			}
		}

		log.jGroupsConfigurationNotFoundInProperties( props );
		try {
			URL fileUrl = ConfigurationParseHelper.locateConfig( JGroupsChannelProvider.DEFAULT_JGROUPS_CONFIGURATION_FILE );
			if ( fileUrl != null ) {
				log.startingJGroupsChannel( fileUrl );
				return new ManagedChannelContainer( new JChannel( fileUrl ), clusterName );
			}
			else {
				log.jGroupsDefaultConfigurationFileNotFound();
				return new ManagedChannelContainer( new JChannel(), clusterName );
			}
		}
		catch (Exception e) {
			throw log.unableToStartJGroupsChannel( e );
		}
	}

	private interface ChannelContainer {
		JChannel getChannel();
		void close();
		void start();
	}

	private static class ManagedChannelContainer implements ChannelContainer {
		private final JChannel channel;
		private final String clusterName;

		ManagedChannelContainer(JChannel channel, String clusterName) {
			if ( channel == null ) {
				throw new NullPointerException( "channel must not be null" );
			}
			if ( clusterName == null ) {
				throw new NullPointerException( "clusterName must not be null" );
			}
			this.channel = channel;
			this.clusterName = clusterName;
		}

		@Override
		public JChannel getChannel() {
			return channel;
		}

		@Override
		public void close() {
			log.jGroupsDisconnectingAndClosingChannel( clusterName );
			channel.disconnect();
			channel.close();
		}

		@Override
		public void start() {
			try {
				channel.connect( clusterName );
				log.jGroupsConnectedToCluster( clusterName, channel.getAddress() );
			}
			catch (Exception e) {
				throw log.unableConnectingToJGroupsCluster( clusterName, e );
			}
		}
	}

	private static class InjectedChannelContainer implements ChannelContainer {

		private final JChannel channel;

		InjectedChannelContainer(JChannel channel) {
			if ( channel == null ) {
				throw new NullPointerException( "channel must not be null" );
			}
			this.channel = channel;
		}

		@Override
		public JChannel getChannel() {
			return channel;
		}

		@Override
		public void close() {
			//No-Op
		}

		@Override
		public void start() {
			//No-Op
		}
	}

}
