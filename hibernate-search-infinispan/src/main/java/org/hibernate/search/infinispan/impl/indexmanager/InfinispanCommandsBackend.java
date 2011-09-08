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
package org.hibernate.search.infinispan.impl.indexmanager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.CacheManagerServiceProvider;
import org.hibernate.search.infinispan.InfinispanIntegration;
import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.Cache;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;

/**
 * An Hibernate Search backend module tailored to delegate index writing to
 * another node in the Infinispan cluster using custom remoting commands.
 *
 * The master IndexWriter for each index is elected as a consistent hash
 * on the index name. Index shards in Hibernate Search have different names,
 * so this provides a primitive load balancing.
 *
 * Delegating to another node is simple, but we need to setup incoming RPC
 * processing capabilities as well.
 *
 * TODO Design a policy to deterministically try balance different shards of
 * the same index on different nodes.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class InfinispanCommandsBackend implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make( Log.class );

	private EmbeddedCacheManager cacheManager;
	private ServiceManager services;
	private String indexName;
	private DistributionManager distributionManager;
	private RpcManager rpcManager;
	private String cacheName;
	private DirectoryBasedIndexManager indexManager;
	private Cache<Object, Object> channeledCache;
	private CacheManagerMuxer cacheMuxer;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.services = context.getServiceManager();
		this.indexManager = indexManager;
		this.cacheManager = services.requestService( CacheManagerServiceProvider.class, context );
		this.indexName = indexManager.getIndexName();
		final String channeledCacheName = InfinispanIntegration.getMetadataCacheName( props );
		this.channeledCache = cacheManager.getCache( channeledCacheName );
		final ComponentRegistry componentsRegistry = channeledCache.getAdvancedCache().getComponentRegistry();
		this.cacheMuxer = componentsRegistry.getComponent( CacheManagerMuxer.class );
		this.rpcManager = componentsRegistry.getComponent( RpcManager.class );
		this.cacheName = componentsRegistry.getCacheName();
		this.distributionManager = componentsRegistry.getComponent( DistributionManager.class );
	}

	@Override
	public void close() {
		cacheMuxer.disableIndexManager( indexName );
		services.releaseService( CacheManagerServiceProvider.class );
		services = null;
		cacheManager = null;
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		IndexUpdateCommand command = new IndexUpdateCommand( cacheName );
		// Use Search's custom Avro based serializer as it includes support for back/future compatibility
		byte[] serializedModel = indexManager.getSerializer().toSerializedModel( workList );
		command.setMessage( serializedModel );
		command.setIndexName( this.indexName );
		sendCommand( command, workList );
	}

	private void sendCommand(ReplicableCommand command, List<LuceneWork> workList) {
		Address primaryNodeAddress = getPrimaryNodeAddress();
		Collection<Address> recipients = Collections.singleton( primaryNodeAddress );
		rpcManager.invokeRemotely( recipients, command, false );
		log.workListRemotedTo( workList, primaryNodeAddress );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		applyWork( Collections.singletonList( singleOperation ), monitor );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		throw new UnsupportedOperationException( "Not Implementable: nonsense on a distributed index." );
	}

	@Override
	public void indexMappingChanged() {
		// FIXME implement me? Need to think if we really need it.
	}

	public boolean isMasterLocal() {
		Transport transport = cacheManager.getTransport();
		if ( transport == null ) {
			return true;
		}
		else {
			final Address primaryLocation = getPrimaryNodeAddress();
			Address localAddress = transport.getAddress();
			return localAddress.equals( primaryLocation );
		}
	}

	/**
	 * Returns the primary node for this index, or null
	 * for non clustered configurations.
	 */
	private Address getPrimaryNodeAddress() {
		Transport transport = cacheManager.getTransport();
		if ( transport == null ) {
			return null;
		}
		if ( distributionManager == null ) { // If cache is configured as REPL
			// TODO Make this decision making pluggable?
			List<Address> members = transport.getMembers();
			int elementIndex = ( Math.abs( indexName.hashCode() ) % members.size() );
			return members.get( elementIndex );
		}
		else { // If cache is configured as DIST
			return distributionManager.getPrimaryLocation( indexName );
		}
	}

	/**
	 * While initializing components the Infinispan cluster is forming and we might already
	 * be elected by other nodes as "master" while unable to process the incoming commands.
	 * Therefore the initialization is split in two phases, and only as last step we advertise
	 * our processing capabilities. This race condition is very unlikely and can only happen
	 * during node initialization, still I prefer to defent against it.
	 */
	void enableIncomingRPCs() {
		cacheMuxer.activateIndexManager( indexName, indexManager );
	}

}
