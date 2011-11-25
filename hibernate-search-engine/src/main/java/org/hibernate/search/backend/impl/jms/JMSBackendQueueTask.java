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
package org.hibernate.search.backend.impl.jms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JMSBackendQueueTask implements Runnable {

	private static final Log log = LoggerFactory.make();

	public static final String INDEX_NAME_JMS_PROPERTY = "HSearchIndexName";

	private final Collection<LuceneWork> queue;
	private final JMSBackendQueueProcessor processor;
	private final String indexName;
	private final IndexManager indexManager;

	public JMSBackendQueueTask(String indexName, Collection<LuceneWork> queue, IndexManager indexManager,
					JMSBackendQueueProcessor jmsBackendQueueProcessor) {
		this.indexName = indexName;
		this.queue = queue;
		this.indexManager = indexManager;
		this.processor = jmsBackendQueueProcessor;
	}

	public void run() {
		List<LuceneWork> filteredQueue = new ArrayList<LuceneWork>(queue);
		for (LuceneWork work : queue) {
			if ( work instanceof OptimizeLuceneWork ) {
				//we don't want optimization to be propagated
				filteredQueue.remove( work );
			}
		}
		if ( filteredQueue.size() == 0) return;
		LuceneWorkSerializer serializer = indexManager.getSerializer();
		byte[] data = serializer.toSerializedModel( filteredQueue );
		processor.prepareJMSTools();
		QueueConnection cnn = null;
		QueueSender sender;
		QueueSession session;
		try {
			cnn = processor.getJMSFactory().createQueueConnection();
			//TODO make transacted parameterized
			session = cnn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );
			ObjectMessage message = session.createObjectMessage();
			message.setObject( data );
			message.setStringProperty( INDEX_NAME_JMS_PROPERTY, indexName );

			sender = session.createSender( processor.getJmsQueue() );
			sender.send( message );

			session.close();
		}
		catch (JMSException e) {
			throw new SearchException( "Unable to send Search work to JMS queue: " + processor.getJmsQueueName(), e );
		}
		finally {
			try {
				if (cnn != null)
					cnn.close();
				}
			catch ( JMSException e ) {
				log.unableToCloseJmsConnection( processor.getJmsQueueName(), e );
			}
		}
	}
}
