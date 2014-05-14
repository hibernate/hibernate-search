/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JmsBackendQueueTask implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final Collection<LuceneWork> queue;
	private final JmsBackendQueueProcessor processor;
	private final String indexName;
	private final IndexManager indexManager;

	public JmsBackendQueueTask(String indexName, Collection<LuceneWork> queue, IndexManager indexManager,
					JmsBackendQueueProcessor jmsBackendQueueProcessor) {
		this.indexName = indexName;
		this.queue = queue;
		this.indexManager = indexManager;
		this.processor = jmsBackendQueueProcessor;
	}

	@Override
	public void run() {
		List<LuceneWork> filteredQueue = new ArrayList<LuceneWork>(queue);
		for ( LuceneWork work : queue ) {
			if ( work instanceof OptimizeLuceneWork ) {
				//we don't want optimization to be propagated
				filteredQueue.remove( work );
			}
		}
		if ( filteredQueue.size() == 0 ) {
			return;
		}
		LuceneWorkSerializer serializer = indexManager.getSerializer();
		byte[] data = serializer.toSerializedModel( filteredQueue );
		QueueSender sender;
		QueueSession session;
		QueueConnection connection;
		try {
			connection = processor.getJMSConnection();
			//TODO make transacted parameterized
			session = connection.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );
			ObjectMessage message = session.createObjectMessage();
			message.setObject( data );
			message.setStringProperty( Environment.INDEX_NAME_JMS_PROPERTY, indexName );

			sender = session.createSender( processor.getJmsQueue() );
			sender.send( message );

			session.close();
		}
		catch (JMSException e) {
			throw log.unableToSendJMSWork( indexName, processor.getJmsQueueName(), e );
		}
	}
}
