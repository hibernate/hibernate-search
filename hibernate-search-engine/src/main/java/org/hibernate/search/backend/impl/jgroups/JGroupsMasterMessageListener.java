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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.Log;
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

	private static final Log log = LoggerFactory.make();
	private final BuildContext context;
	private final NodeSelectorStrategyHolder selector;
    private final ClassLoader classLoader;

	public JGroupsMasterMessageListener(BuildContext context, NodeSelectorStrategyHolder masterNodeSelector) {
        this(context, masterNodeSelector, null);
	}

	public JGroupsMasterMessageListener(BuildContext context, NodeSelectorStrategyHolder masterNodeSelector, ClassLoader classLoader) {
		this.context = context;
		this.selector = masterNodeSelector;
        this.classLoader = classLoader;
	}

	@Override
	public void receive(Message message) {
		final byte[] rawBuffer = message.getRawBuffer();
		final String indexName = MessageSerializationHelper.extractIndexName( rawBuffer );
        receive(message, rawBuffer, indexName);
	}

    protected void receive(Message message, byte[] rawBuffer, String indexName) {
        final NodeSelectorStrategy nodeSelector = selector.getMasterNodeSelector( indexName );
        try {
            if ( nodeSelector.isIndexOwnerLocal() ) {
                byte[] serializedQueue = MessageSerializationHelper.extractSerializedQueue(rawBuffer);
                final IndexManager indexManager = context.getAllIndexesManager().getIndexManager( indexName );
                if ( indexManager != null ) {
                    applyToIndexManager(message, serializedQueue, indexManager);
                }
                else {
                    log.messageReceivedForUndefinedIndex( indexName );
                }
            }
            else {
                //TODO forward to new owner or log error
            }
        }
        catch ( ClassCastException e ) {
            log.illegalObjectRetrievedFromMessage( e );
        }
        catch ( SearchException e ) {
            log.illegalObjectRetrievedFromMessage( e );
        }
    }

    private void applyToIndexManager(Message message, byte[] serializedQueue, IndexManager indexManager) {
        if (classLoader != null) {
            final ClassLoader previous = SecurityActions.setTCCL(classLoader);
            try {
                applyToIndexManagerInternal(message, serializedQueue, indexManager);
            } finally {
                SecurityActions.setTCCL(previous);
            }
        } else {
            applyToIndexManagerInternal(message, serializedQueue, indexManager);
        }
    }

    private void applyToIndexManagerInternal(Message message, byte[] serializedQueue, IndexManager indexManager) {
        final List<LuceneWork> queue = indexManager.getSerializer().toLuceneWorks( serializedQueue );
        applyLuceneWorkLocally( queue, indexManager, message );
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
			log.receivedEmptyLuceneWOrksInMessage();
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
