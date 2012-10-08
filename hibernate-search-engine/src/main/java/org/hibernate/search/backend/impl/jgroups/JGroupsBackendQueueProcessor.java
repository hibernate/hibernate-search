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

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.Address;

/**
 * This index backend is able to switch dynamically between a standard
 * Lucene index writing backend and one which sends work remotely over
 * a JGroups channel.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Ales Justin
 */
public class JGroupsBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private final NodeSelectorStrategy selectionStrategy;

	protected MessageSender messageSender;
	protected String indexName;
	protected DirectoryBasedIndexManager indexManager;

	private Address address;
	private ServiceManager serviceManager;

	private JGroupsBackendQueueTask jgroupsProcessor;
	private LuceneBackendQueueProcessor luceneBackendQueueProcessor;


	public JGroupsBackendQueueProcessor(NodeSelectorStrategy selectionStrategy) {
		this.selectionStrategy = selectionStrategy;
	}

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.indexName = indexManager.getIndexName();
		assertLegacyOptionsNotUsed( props, indexName );
		this.indexManager = indexManager;
		serviceManager = context.getServiceManager();
		this.messageSender = serviceManager.requestService( JGroupsChannelProvider.class, context );
		NodeSelectorStrategyHolder masterNodeSelector = serviceManager.requestService( MasterSelectorServiceProvider.class, context );
		masterNodeSelector.setNodeSelectorStrategy( indexName, selectionStrategy );
		selectionStrategy.viewAccepted( messageSender.getView() ); // set current view?
		jgroupsProcessor = new JGroupsBackendQueueTask( this, indexManager, masterNodeSelector );
		luceneBackendQueueProcessor = new LuceneBackendQueueProcessor();
		luceneBackendQueueProcessor.initialize( props, context, indexManager );
	}

	public void close() {
		serviceManager.releaseService( MasterSelectorServiceProvider.class );
		serviceManager.releaseService( JGroupsChannelProvider.class );
		luceneBackendQueueProcessor.close();
	}

	MessageSender getMessageSender() {
		return messageSender;
	}

	/**
	 * Cluster's node address
	 *
	 * @return Address
	 */
	public Address getAddress() {
		if ( address == null && messageSender != null ) {
			address = messageSender.getAddress();
		}
		return address;
	}

	@Override
	public void indexMappingChanged() {
		// no-op
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			luceneBackendQueueProcessor.applyWork( workList, monitor );
		}
		else {
			if ( workList == null ) {
				throw new IllegalArgumentException( "workList should not be null" );
			}
			jgroupsProcessor.sendLuceneWorkList( workList );
		}
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			luceneBackendQueueProcessor.applyStreamWork( singleOperation, monitor );
		}
		else {
			//TODO optimize for single operation?
			jgroupsProcessor.sendLuceneWorkList( Collections.singletonList( singleOperation ) );
		}
	}

	@Override
	public Lock getExclusiveWriteLock() {
		return luceneBackendQueueProcessor.getExclusiveWriteLock();
	}

	private static void assertLegacyOptionsNotUsed(Properties props, String indexName) {
		MaskedProperty jgroupsCfg = new MaskedProperty( props, "worker.backend.jgroups" );
		if ( jgroupsCfg.containsKey( "configurationFile" )
				|| jgroupsCfg.containsKey( "configurationXml" )
				|| jgroupsCfg.containsKey( "configurationString" )
				|| jgroupsCfg.containsKey( "clusterName" ) ) {
			throw log.legacyJGroupsConfigurationDefined( indexName );
		}
	}
}
