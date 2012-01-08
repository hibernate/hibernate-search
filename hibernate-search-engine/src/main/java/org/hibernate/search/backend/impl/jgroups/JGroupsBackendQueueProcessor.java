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
import java.util.Properties;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;

import org.hibernate.search.Environment;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.XMLHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Common base class for Master and Slave BackendQueueProcessorFactories
 *
 * @author Lukasz Moren
 */
public abstract class JGroupsBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	public static final String JGROUPS_PREFIX = Environment.WORKER_BACKEND + ".jgroups.";

	public static final String CONFIGURATION_STRING = JGROUPS_PREFIX + "configurationString";
	public static final String CONFIGURATION_XML = JGROUPS_PREFIX + "configurationXml";
	public static final String CONFIGURATION_FILE = JGROUPS_PREFIX + "configurationFile";
	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";

	public static final String JG_CLUSTER_NAME = JGROUPS_PREFIX + "clusterName";

	protected String clusterName = "HSearchCluster";
	protected Channel channel = null;
	protected Address address;
	protected String indexName;
	protected DirectoryBasedIndexManager indexManager;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.indexManager = indexManager;
		indexName = indexManager.getIndexName();

		if ( props.containsKey( JG_CLUSTER_NAME ) ) {
			setClusterName( props.getProperty( JG_CLUSTER_NAME ) );
		}
		prepareJGroupsChannel( props );
	}

	private void prepareJGroupsChannel(Properties props) {
		log.jGroupsStartingChannel();
		try {
			buildChannel( props );
			channel.connect( clusterName );
		}
		catch ( Exception e ) {
			throw log.unabletoConnectToJGroupsCluster( clusterName, e );
		}
		log.jGroupsConnectedToCluster(clusterName, getAddress() );

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
			if ( props.containsKey( CONFIGURATION_FILE ) ) {
				cfg = props.getProperty( CONFIGURATION_FILE );
				try {
					channel = new JChannel( ConfigurationParseHelper.locateConfig(cfg) );
				}
				catch ( Exception e ) {
					throw log.jGroupsChannelCreationUsingFileError( cfg, e );
				}
			}

			if ( props.containsKey( CONFIGURATION_XML ) ) {
				cfg = props.getProperty( CONFIGURATION_XML );
				try {
					channel = new JChannel( XMLHelper.elementFromString( cfg ) );
				}
				catch ( Exception e ) {
					throw log.jGroupsChannelCreationUsingXmlError( cfg, e );
				}
			}

			if ( props.containsKey( CONFIGURATION_STRING ) ) {
				cfg = props.getProperty( CONFIGURATION_STRING );
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
				URL fileUrl = ConfigurationParseHelper.locateConfig( DEFAULT_JGROUPS_CONFIGURATION_FILE );
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

	public void close() {
		try {
			if ( channel != null && channel.isOpen() ) {
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
			address = channel.getAddress();
		}
		return address;
	}
}
