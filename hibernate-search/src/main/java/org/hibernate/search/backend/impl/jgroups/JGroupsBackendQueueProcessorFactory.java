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
package org.hibernate.search.backend.impl.jgroups;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.slf4j.Logger;

import org.hibernate.search.Environment;
import org.hibernate.search.backend.UpdatableBackendQueueProcessorFactory;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.JGroupsHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.XMLHelper;
import org.hibernate.util.ConfigHelper;


/**
 * Common base class for Master and Slave BackendQueueProcessorFactories
 *
 * @author Lukasz Moren
 */
public abstract class JGroupsBackendQueueProcessorFactory implements UpdatableBackendQueueProcessorFactory {

	private static final Logger log = LoggerFactory.make();

	public static final String JGROUPS_PREFIX = Environment.WORKER_BACKEND + ".jgroups.";

	public static final String CONFIGURATION_STRING = JGROUPS_PREFIX + "configurationString";
	public static final String CONFIGURATION_XML = JGROUPS_PREFIX + "configurationXml";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";

	public static final String JG_CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";

	protected String clusterName = "HSearchCluster";
	protected SearchFactoryImplementor searchFactory;
	protected Channel channel = null;
	protected Address address;

	public void initialize(Properties props, WorkerBuildContext context) {
		JGroupsHelper.verifyIPv4IsPreferred();
		this.searchFactory = context.getUninitializedSearchFactory();

		if ( props.containsKey( JG_CLUSTER_NAME ) ) {
			setClusterName( props.getProperty( JG_CLUSTER_NAME ) );
		}
		prepareJGroupsChannel( props );
	}

	public void updateDirectoryProviders(Set<DirectoryProvider<?>> providers, WorkerBuildContext context) {
		//nothing to do here. The DirectoryProviders are not used
	}

	private void prepareJGroupsChannel(Properties props) {
		log.info( "Starting JGroups Channel" );
		try {
			buildChannel( props );
			channel.setOpt( Channel.AUTO_RECONNECT, Boolean.TRUE );
			channel.connect( clusterName );
		}
		catch ( ChannelException e ) {
			throw new SearchException( "Unable to connect to: [" + clusterName + "] JGroups channel" );
		}
		log.info( "Connected to cluster [ {} ]. The node address is {}", clusterName, getAddress() );

		if ( !channel.flushSupported() ) {
			log.warn(
					"FLUSH is not present in your JGroups stack!  FLUSH is needed to ensure messages are not dropped while new nodes join the cluster.  Will proceed, but inconsistencies may arise!"
			);
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
			if ( props.containsKey( CONFIGURATION_FILE ) ) {
				cfg = props.getProperty( CONFIGURATION_FILE );
				try {
					channel = new JChannel( ConfigHelper.locateConfig( cfg ) );
				}
				catch ( Exception e ) {
					log.error( "Error while trying to create a channel using config files: {}", cfg );
					throw new SearchException( e );
				}
			}

			if ( props.containsKey( CONFIGURATION_XML ) ) {
				cfg = props.getProperty( CONFIGURATION_XML );
				try {
					channel = new JChannel( XMLHelper.elementFromString( cfg ) );
				}
				catch ( Exception e ) {
					log.error( "Error while trying to create a channel using config XML: {}", cfg );
					throw new SearchException( e );
				}
			}

			if ( props.containsKey( CONFIGURATION_STRING ) ) {
				cfg = props.getProperty( CONFIGURATION_STRING );
				try {
					channel = new JChannel( cfg );
				}
				catch ( Exception e ) {
					log.error( "Error while trying to create a channel using config string: {}", cfg );
					throw new SearchException( e );
				}
			}
		}

		if ( channel == null ) {
			log.info(
					"Unable to use any JGroups configuration mechanisms provided in properties {}. Using default JGroups configuration file!",
					props
			);
			try {
				URL fileUrl = ConfigHelper.locateConfig( DEFAULT_JGROUPS_CONFIGURATION_FILE );
				if ( fileUrl != null ) {
					channel = new JChannel( fileUrl );
				}
				else {
					log.warn(
							"Default JGroups configuration file was not found. Attempt to start JGroups channel with default configuration!"
					);
					channel = new JChannel();
				}
			}
			catch ( ChannelException e ) {
				throw new SearchException( "Unable to start JGroups channel", e );
			}
		}
	}

	public abstract Runnable getProcessor(List<LuceneWork> queue);

	public void close() {
		try {
			if ( channel != null && channel.isOpen() ) {
				log.info( "Disconnecting and closing JGroups Channel" );
				channel.disconnect();
				channel.close();
			}
		}
		catch ( Exception toLog ) {
			log.error( "Problem closing channel; setting it to null", toLog );
			channel = null;
		}
	}

	public Channel getChannel() {
		return channel;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getClusterName() {
		return clusterName;
	}

	/**
	 * Cluster's node address
	 *
	 * @return Address
	 */
	public Address getAddress() {
		if ( address == null && channel != null ) {
			address = channel.getLocalAddress();
		}
		return address;
	}
}
