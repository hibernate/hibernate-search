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

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Message;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Responsible for sending Lucene works from slave nodes to master node
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsBackendQueueTask {

	private static final Log log = LoggerFactory.make();

	private final JGroupsBackendQueueProcessor factory;
	private final String indexName;
	private final IndexManager indexManager;
	private final NodeSelectorStrategy masterNodeSelector;

	public JGroupsBackendQueueTask(JGroupsBackendQueueProcessor factory, IndexManager indexManager, NodeSelectorStrategyHolder masterNodeSelector) {
		this.factory = factory;
		this.indexManager = indexManager;
		this.indexName = indexManager.getIndexName();
		this.masterNodeSelector = masterNodeSelector.getMasterNodeSelector( indexName );
	}

	public void sendLuceneWorkList(List<LuceneWork> queue) {
		boolean trace = log.isTraceEnabled();
		List<LuceneWork> filteredQueue = new ArrayList<LuceneWork>( queue );
		if ( trace ) {
			log.tracef( "Preparing %d Lucene works to be sent to master node.", filteredQueue.size() );
		}

		for ( LuceneWork work : queue ) {
			if ( work instanceof OptimizeLuceneWork ) {
				//TODO might be correct to do, but should be filtered earlier, and skipped server-side.
				//we don't want optimization to be propagated
				filteredQueue.remove( work );
			}
		}
		if ( trace ) {
			log.tracef(
				"Filtering: optimized Lucene works are not going to be sent to master node. There is %d Lucene works after filtering.",
				filteredQueue.size()
			);
		}
		if ( filteredQueue.isEmpty() ) {
			if ( trace ) {
				log.trace( "Nothing to send. Propagating works to a cluster has been skipped." );
			}
			return;
		}
		byte[] data = indexManager.getSerializer().toSerializedModel( filteredQueue );
		data = MessageSerializationHelper.prependString( indexName, data );

		try {
			Message message =  masterNodeSelector.createMessage( data );
			factory.getChannel().send( message );
			if ( trace ) {
				log.tracef( "Lucene works have been sent from slave %s to master node.", factory.getAddress() );
			}
		}
		catch ( Exception e ) {
			throw log.unableToSendWorkViaJGroups( e );
		}
	}

}
