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

import java.util.List;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Listen for messages from slave nodes and apply them into <code>LuceneBackendQueueProcessor</code>
 *
 * @author Lukasz Moren
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor
 * @see org.jgroups.Receiver
 */
public class JGroupsMasterMessageListener implements Receiver {

	private static final Log log = LoggerFactory.make();

	private SearchFactoryImplementor searchFactory;

	public JGroupsMasterMessageListener(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
	}

	@SuppressWarnings("unchecked")
	public void receive(Message message) {
		List<LuceneWork> queue;
		try {
			queue = ( List<LuceneWork> ) message.getObject();
		}
		catch ( ClassCastException e ) {
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
			Runnable worker = getWorker( queue );
			worker.run();
		}
		else {
			log.receivedEmptyLuceneWOrksInMessage();
		}
	}

	private Runnable getWorker(List<LuceneWork> queue) {
		Runnable processor;
		processor = searchFactory.getBackendQueueProcessorFactory().getProcessor( queue );
		return processor;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Implementations of JGroups interfaces
	// ------------------------------------------------------------------------------------------------------------------

	public byte[] getState() {
		return null;
	}

	public void setState(byte[] state) {
		//no-op
	}

	public void viewAccepted(View view) {
		log.jGroupsReceivedNewClusterView( view );
	}

	public void suspect(Address suspected_mbr) {
		//no-op
	}

	public void block() {
		//no-op
	}
}
