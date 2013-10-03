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
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.Cache;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;

/**
 * An Hibernate Search backend module tailored to delegate index writing to
 * another node in the Infinispan cluster using custom remoting commands.
 *
 * This is just one side of the picture: a service to receive the commands
 * must be registered as well, and such a service must be ready to receive
 * commands before another node decides to send them to it.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class InfinispanCommandsBackend implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make( Log.class );

	private String indexName;
	private RpcManager rpcManager;
	private String cacheName;
	private DirectoryBasedIndexManager indexManager;

	private final Address masterNode;
	private final Collection<Address> recipients;
	private final Cache channeledCache;


	public InfinispanCommandsBackend(Address masterNode, Cache channeledCache) {
		this.masterNode = masterNode;
		this.channeledCache = channeledCache;
		this.recipients = Collections.singleton( masterNode );
	}

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.indexManager = indexManager;
		this.indexName = indexManager.getIndexName();
		final ComponentRegistry componentsRegistry = channeledCache.getAdvancedCache().getComponentRegistry();
		this.rpcManager = componentsRegistry.getComponent( RpcManager.class );
		this.cacheName = componentsRegistry.getCacheName();
	}

	@Override
	public void close() {
		//all components are managed externally: nothing to close here.
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
		rpcManager.invokeRemotely( recipients, command, rpcManager.getDefaultRpcOptions( true ) );
		log.workListRemotedTo( workList, masterNode );
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

}
