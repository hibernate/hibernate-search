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
package org.hibernate.search.test.jms.slave;

import java.util.List;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.JMSException;


import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.JmsBackendQueueTask;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Helper class to verify that the Slave places messages onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SearchQueueChecker implements MessageListener {
	public static int queues;
	public static int works;
	private SearchFactoryImplementor searchFactory;

	public SearchQueueChecker(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
	}

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onMessage(Message message) {
		if ( !( message instanceof ObjectMessage ) ) {
			return;
		}
		ObjectMessage objectMessage = (ObjectMessage) message;

		List<LuceneWork> queue;
		try {
			String indexName = objectMessage.getStringProperty( JmsBackendQueueTask.INDEX_NAME_JMS_PROPERTY );
			IndexManager indexManager = searchFactory.getIndexManagerHolder().getIndexManager( indexName );
			queue = indexManager.getSerializer().toLuceneWorks( (byte[]) objectMessage.getObject() );
		}
		catch (JMSException e) {
			return;
		}
		catch (ClassCastException e) {
			return;
		}
		queues++;
		works += queue.size();
	}
}
