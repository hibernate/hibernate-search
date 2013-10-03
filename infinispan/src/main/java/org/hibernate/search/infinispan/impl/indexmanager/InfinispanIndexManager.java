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

import java.util.Properties;

import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.CacheManagerServiceProvider;
import org.hibernate.search.infinispan.InfinispanIntegration;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A custom IndexManager to store indexes in the grid itself.
 * This IndexManager creates an indexing backend able to automatically
 * elect a master node and forward indexing commands to the appropriate
 * node, so that no configuration is required for the backend.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class InfinispanIndexManager extends DirectoryBasedIndexManager {

	private static final Log log = LoggerFactory.make( Log.class );

	private InfinispanCommandsBackend remoteMaster;

	private ServiceManager serviceManager;

	private EmbeddedCacheManager cacheManager;

	private CacheManagerMuxer cacheMuxer;

	private String indexName;

	private Cache channeledCache;

	@Override
	public void initialize(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		this.indexName = indexName;
		this.serviceManager = buildContext.getServiceManager();
		this.cacheManager = serviceManager.requestService( CacheManagerServiceProvider.class, buildContext );
		final String channeledCacheName = InfinispanIntegration.getMetadataCacheName( cfg );
		channeledCache = cacheManager.getCache( channeledCacheName );
		final ComponentRegistry componentsRegistry = channeledCache.getAdvancedCache().getComponentRegistry();
		this.cacheMuxer = componentsRegistry.getComponent( CacheManagerMuxer.class );
		super.initialize( indexName, cfg, buildContext );
		//needs to happen last: opens the gates and let the lions in:
		cacheMuxer.activateIndexManager( indexName, this );
	}

	@Override
	public void destroy() {
		super.destroy();
		cacheMuxer.disableIndexManager( indexName );
		serviceManager.releaseService( CacheManagerServiceProvider.class );
	}

	@Override
	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		if ( cacheManager.getTransport() == null ) {
			//not capable of remoting: just return the standard local-only backend
			return super.createBackend( indexName, cfg, buildContext );
		}
		else {
			RoutingArbiter arbiter = new RoutingArbiter( this, cfg, indexName, channeledCache );
			arbiter.initialize( cfg, buildContext, this );
			return arbiter;
		}
	}

	protected DirectoryProvider<InfinispanDirectory> createDirectoryProvider(
			String indexName, Properties cfg, WorkerBuildContext buildContext) {
		// warn user we're overriding the configured DirectoryProvider if he has explicitly set anything different than Infinispan
		String directoryOption = cfg.getProperty( "directory_provider", null );
		if ( directoryOption != null && !"infinispan".equals( directoryOption ) ) {
			log.ignoreDirectoryProviderProperty( indexName, directoryOption );
		}
		InfinispanDirectoryProvider infinispanDP = new InfinispanDirectoryProvider();
		infinispanDP.initialize( indexName, cfg, buildContext );
		return infinispanDP;
	}

	public InfinispanCommandsBackend getRemoteMaster() {
		return remoteMaster;
	}

	public EmbeddedCacheManager getCacheManager() {
		return this.cacheManager;
	}

}
