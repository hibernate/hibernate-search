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
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Listen for messages from slave nodes and apply them into <code>LuceneBackendQueueProcessor</code>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueTask
 * @see org.jgroups.Receiver
 */
public class JGroupsMasterMessageListener implements Receiver {

	private static final Log log = LoggerFactory.make();

	private SearchFactoryImplementor searchFactory;

	public JGroupsMasterMessageListener(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(Message message) {
		final List<LuceneWork> queue;
		final String indexName;
		final IndexManager indexManager;
		try {
			BackendMessage decoded = (BackendMessage) message.getObject();
			indexName = decoded.indexName;
			indexManager = searchFactory.getAllIndexesManager().getIndexManager( indexName );
			if ( indexManager != null ) {
				queue = indexManager.getSerializer().toLuceneWorks( decoded.queue );
			}
			else {
				log.messageReceivedForUndefinedIndex( indexName );
				return;
			}
		}
		catch ( ClassCastException e ) {
			log.illegalObjectRetrievedFromMessage( e );
			return;
		}
		catch ( SearchException e ) {
			log.illegalObjectRetrievedFromMessage( e );
			return;
		}

		if ( queue != null && !queue.isEmpty() ) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"There are %d Lucene docs received from slave node %s to be processed by master",
						queue.size(),
						message.getSrc()
				);
			}
			perform( indexName, queue );
		}
		else {
			log.receivedEmptyLuceneWOrksInMessage();
		}
	}

	private void perform(String indexName, List<LuceneWork> queue) {
		IndexManagerHolder allIndexesManager = searchFactory.getAllIndexesManager();
		IndexManager indexManager = allIndexesManager.getIndexManager( indexName );
		indexManager.performOperations( queue, null );
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Implementations of JGroups interfaces
	// ------------------------------------------------------------------------------------------------------------------

	@Override
	public void viewAccepted(View view) {
		log.jGroupsReceivedNewClusterView( view );
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
