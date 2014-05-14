/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Listen for messages from slave nodes and apply them into <code>LuceneBackendQueueProcessor</code>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Ales Justin
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueTask
 * @see org.jgroups.Receiver
 */
public class JGroupsMasterMessageListener implements Receiver {

	private static final Log log = LoggerFactory.make( Log.class );

	private final BuildContext context;
	private final NodeSelectorService selector;

	public JGroupsMasterMessageListener(BuildContext context, NodeSelectorService masterNodeSelector) {
		this.context = context;
		this.selector = masterNodeSelector;
	}

	@Override
	public void receive(Message message) {
		final int offset = message.getOffset();
		final int bufferLength = message.getLength();
		final byte[] rawBuffer = message.getRawBuffer();
		final String indexName = MessageSerializationHelper.extractIndexName( offset, rawBuffer );
		final NodeSelectorStrategy nodeSelector = selector.getMasterNodeSelector( indexName );
		try {
			//nodeSelector can be null if we receive the message during shutdown
			if ( nodeSelector != null && nodeSelector.isIndexOwnerLocal() ) {
				byte[] serializedQueue = MessageSerializationHelper.extractSerializedQueue( offset, bufferLength, rawBuffer );
				final IndexManager indexManager = context.getAllIndexesManager().getIndexManager( indexName );
				if ( indexManager != null ) {
					final List<LuceneWork> queue = indexManager.getSerializer().toLuceneWorks( serializedQueue );
					applyLuceneWorkLocally( queue, indexManager, message );
				}
				else {
					log.messageReceivedForUndefinedIndex( indexName );
				}
			}
			else {
				//TODO forward to new owner or log error
			}
		}
		catch (ClassCastException e) {
			log.illegalObjectRetrievedFromMessage( e );
		}
		catch (SearchException e) {
			log.illegalObjectRetrievedFromMessage( e );
		}
	}

	private void applyLuceneWorkLocally(List<LuceneWork> queue, IndexManager indexManager, Message message) {
		if ( queue != null && !queue.isEmpty() ) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"There are %d Lucene docs received from slave node %s to be processed by master",
						queue.size(),
						message.getSrc()
				);
			}
			indexManager.performOperations( queue, null );
		}
		else {
			log.receivedEmptyLuceneWorksInMessage();
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Implementations of JGroups interfaces
	// ------------------------------------------------------------------------------------------------------------------

	@Override
	public void viewAccepted(View view) {
		log.jGroupsReceivedNewClusterView( view );
		selector.viewAccepted( view );
	}

	@Override
	public void suspect(Address suspected_mbr) {
		//no-op
	}

	@Override
	public void block() {
		//no-op
	}

	@Override
	public void getState(OutputStream arg0) throws Exception {
	}

	@Override
	public void setState(InputStream arg0) throws Exception {
	}

	@Override
	public void unblock() {
	}
}
