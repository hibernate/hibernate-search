/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.jgroups.Message;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Responsible for sending Lucene works from slave nodes to master node
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Ales Justin
 */
public class JGroupsBackendQueueTask {

	private static final Log log = LoggerFactory.make( Log.class );

	private final JGroupsBackendQueueProcessor factory;
	private final String indexName;
	private final IndexManager indexManager;
	private final NodeSelectorStrategy masterNodeSelector;
	private final boolean blockForACK; //true by default if this backend is synchronous
	private final long messageTimeout;

	public JGroupsBackendQueueTask(JGroupsBackendQueueProcessor factory, IndexManager indexManager,
			NodeSelectorService masterNodeSelector, boolean blockForACK, long messageTimeout) {
		this.factory = factory;
		this.indexManager = indexManager;
		this.blockForACK = blockForACK;
		this.messageTimeout = messageTimeout;
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
			Message message = masterNodeSelector.createMessage( data );
			factory.getMessageSenderService().send( message, blockForACK, messageTimeout );
			if ( trace ) {
				log.tracef( "Lucene works have been sent from slave %s to master node.", factory.getAddress() );
			}
		}
		catch (Exception e) {
			throw log.unableToSendWorkViaJGroups( e );
		}
	}

	public boolean blocksForACK() {
		return blockForACK;
	}

	public long getMessageTimeout() {
		return messageTimeout;
	}

}
