/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.net.URL;
import java.util.Properties;

import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message.TransientFlag;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.util.Buffer;
import org.jgroups.util.RspList;

/**
 * We use the MessageDispatcher instead of the JChannel to be able to use blocking
 * operations (optionally) without having to rely on the RSVP protocol
 * being configured on the stack.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @author Ales Justin
 * @author Hardy Ferentschik
 */
public final class DispatchMessageSender implements MessageSenderService, Startable, Stoppable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String JGROUPS_PREFIX = "hibernate.search.services.jgroups.";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	public static final String CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";
	public static final String CHANNEL_INJECT = JGROUPS_PREFIX + "providedChannel";
	public static final String CLASSLOADER = JGROUPS_PREFIX + "classloader";

	/**
	 * @deprecated the MUX channels are no longer supported in JGroups 4: this property will be ignored.
	 */
	@Deprecated
	public static final String MUX_ID = JGROUPS_PREFIX + "mux_id";

	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";
	private static final String DEFAULT_CLUSTER_NAME = "Hibernate Search Cluster";

	private ChannelContainer channelContainer;
	private ServiceManager serviceManager;
	private MessageDispatcher dispatcher;

	@Override
	public Address getAddress() {
		return channelContainer.getChannel().getAddress();
	}

	@Override
	public View getView() {
		return channelContainer.getChannel().getView();
	}

	@Override
	public void send(final Buffer data, final boolean synchronous, final long timeout) throws Exception {
		final RequestOptions options = synchronous ? RequestOptions.SYNC() : RequestOptions.ASYNC();
		options.exclusionList( dispatcher.getChannel().getAddress() );
		options.setTransientFlags( TransientFlag.DONT_LOOPBACK );
		options.setTimeout( timeout );
		if ( synchronous ) {
			try {
				RspList<Object> rspList = dispatcher.castMessage( null, data, options );
				handleResponseProblems( rspList, null );
			}
			catch (Exception e) {
				throw log.unableToSendWorkViaJGroups( e );
			}
		}
		else {
			try {
				dispatcher.castMessageWithFuture( null, data, options );
			}
			catch (RuntimeException e) {
				throw log.unableToSendWorkViaJGroups( e );
			}
		}
	}

	private void handleResponseProblems(RspList<Object> response, Throwable e) {
		for ( Address suspected : response.getSuspectedMembers() ) {
			log.jgroupsSuspectingPeer( suspected );
		}
		if ( e != null ) {
			throw log.unableToSendWorkViaJGroups( e );
		}
	}

	@Override
	public void start(Properties props, BuildContext context) {
		log.jGroupsStartingChannelProvider();
		serviceManager = context.getServiceManager();

		channelContainer = buildChannel( props );
		checkForOldProperties( props );

		NodeSelectorService masterNodeSelector = serviceManager.requestService( NodeSelectorService.class );
		LuceneWorkSerializer luceneWorkSerializer = serviceManager.requestService( LuceneWorkSerializer.class );
		JGroupsMasterMessageListener listener = new JGroupsMasterMessageListener( context, masterNodeSelector, luceneWorkSerializer );

		JChannel channel = channelContainer.getChannel();

		MessageListenerToRequestHandlerAdapter requestHandler = new MessageListenerToRequestHandlerAdapter( listener );
		dispatcher = new MessageDispatcher( channel, requestHandler );
		dispatcher.setMembershipListener( listener );
		//Only now that the Dispatcher is installed we can start the channel:
		channelContainer.start();

		masterNodeSelector.setLocalAddress( channel.getAddress() );

		if ( !channel.flushSupported() ) {
			log.jGroupsFlushNotPresentInStack();
		}
		if ( log.isDebugEnabled() ) {
			log.jgroupsFullConfiguration( channel.getProtocolStack().printProtocolSpecAsXML() );
		}
	}

	/**
	 * Verifies if there are any legacy properties which no longer apply
	 *
	 * @param props the configuration
	 */
	private void checkForOldProperties(Properties props) {
		if ( props.get( MUX_ID ) != null ) {
			log.muxIdPropertyIsIgnored();
		}
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( serviceManager::releaseService, NodeSelectorService.class, LuceneWorkSerializer.class );
			serviceManager = null;
			closer.push( dispatcher::stop );
			if ( channelContainer != null ) {
				closer.push( channelContainer::close );
				channelContainer = null;
			}
		}
		catch (RuntimeException toLog) {
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
				props, DispatchMessageSender.CLUSTER_NAME, DEFAULT_CLUSTER_NAME );
		if ( props != null ) {
			final Object channelObject = props.get( DispatchMessageSender.CHANNEL_INJECT );
			if ( channelObject != null ) {
				try {
					return new InjectedChannelContainer( (org.jgroups.JChannel) channelObject );
				}
				catch (ClassCastException e) {
					throw log.jGroupsChannelInjectionError( DispatchMessageSender.CHANNEL_INJECT, e, channelObject.getClass() );
				}
			}

			final String cfg = props.getProperty( DispatchMessageSender.CONFIGURATION_FILE );
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
			URL fileUrl = ConfigurationParseHelper.locateConfig( DispatchMessageSender.DEFAULT_JGROUPS_CONFIGURATION_FILE );
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
