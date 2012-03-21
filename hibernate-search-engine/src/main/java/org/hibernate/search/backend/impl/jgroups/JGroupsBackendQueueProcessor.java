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

import java.util.Properties;

import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.jgroups.Address;
import org.jgroups.Channel;

/**
 * Common base class for Master and Slave BackendQueueProcessorFactories
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
abstract class JGroupsBackendQueueProcessor implements BackendQueueProcessor {

	protected Channel channel;
	protected String indexName;
	protected DirectoryBasedIndexManager indexManager;

	private Address address;
	private WorkerBuildContext context;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.indexManager = indexManager;
		this.context = context;
		this.indexName = indexManager.getIndexName();
		this.channel = context.requestService( JGroupsChannelProvider.class );
	}

	public void close() {
		context.releaseService( JGroupsChannelProvider.class );
	}

	public Channel getChannel() {
		return channel;
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

	@Override
	public void indexMappingChanged() {
		// no-op
	}
}
