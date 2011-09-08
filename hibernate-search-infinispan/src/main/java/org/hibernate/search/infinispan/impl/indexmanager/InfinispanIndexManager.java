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

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.lucene.InfinispanDirectory;

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

	@Override
	public void initialize(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		super.initialize( indexName, cfg, buildContext );
		this.remoteMaster.enableIncomingRPCs(); //needs to happen last: opens the gates and let the lions in
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		//We'll actually create two backend processors and expose them as one wrapping them together:
		BackendQueueProcessor localMaster = BackendFactory.createBackend( this, buildContext, cfg );
		remoteMaster = new InfinispanCommandsBackend();
		remoteMaster.initialize( cfg, buildContext, this );
		// localMaster is already initialized by the BackendFactory
		MasterSwitchDelegatingQueueProcessor joinedMaster = new MasterSwitchDelegatingQueueProcessor( localMaster, remoteMaster );
		return joinedMaster;
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

}
