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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.Message;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.util.LoggerFactory;

/**
 * Responsible for sending Lucene works from slave nodes to master node
 *
 * @author Lukasz Moren
 */
public class JGroupsBackendQueueProcessor implements Runnable {

	private static final Logger log = LoggerFactory.make();

	private final JGroupsBackendQueueProcessorFactory factory;
	private final List<LuceneWork> queue;

	public JGroupsBackendQueueProcessor(List<LuceneWork> queue, JGroupsBackendQueueProcessorFactory factory) {
		this.factory = factory;
		this.queue = queue;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		boolean trace = log.isTraceEnabled();
		List<LuceneWork> filteredQueue = new ArrayList<LuceneWork>( queue );
		if ( trace ) {
			log.trace( "Preparing {} Lucene works to be sent to master node.", filteredQueue.size() );
		}

		for ( LuceneWork work : queue ) {
			if ( work instanceof OptimizeLuceneWork ) {
				//we don't want optimization to be propagated
				filteredQueue.remove( work );
			}
		}
		if ( trace ) {
			log.trace(
				"Filtering: optimized Lucene works are not going to be sent to master node. There is {} Lucene works after filtering.",
				filteredQueue.size()
			);
		}
		if ( filteredQueue.isEmpty() ) {
			if ( trace ) {
				log.trace( "Nothing to send. Propagating works to a cluster has been skipped." );
			}
			return;
		}

		/* Creates and send message with lucene works to master.
		 * As long as message destination address is null, Lucene works will be received by all listeners that implements
		 * org.jgroups.MessageListener interface, multiple master nodes in cluster are allowed. */
		try {
			Message message = new Message( null, factory.getAddress(), ( Serializable ) filteredQueue );
			factory.getChannel().send( message );
			if ( trace ) {
				log.trace( "Lucene works have been sent from slave {} to master node.", factory.getAddress() );
			}
		}
		catch ( ChannelNotConnectedException e ) {
			throw new SearchException(
					"Unable to send Lucene work. Channel is not connected to: "
							+ factory.getClusterName()
			);
		}
		catch ( ChannelClosedException e ) {
			throw new SearchException( "Unable to send Lucene work. Attempt to send message on closed JGroups channel" );
		}
	}
	
}
