/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jms.slave;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * Helper class to verify that the Slave places messages onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SearchQueueChecker implements MessageListener {
	public static int queues;
	public static int works;
	private final SearchIntegrator searchIntegrator;

	public SearchQueueChecker(SearchIntegrator searchIntegrator) {
		this.searchIntegrator = searchIntegrator;
	}

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	public void onMessage(Message message) {
		if ( !( message instanceof ObjectMessage ) ) {
			return;
		}
		ObjectMessage objectMessage = (ObjectMessage) message;

		List<LuceneWork> queue;
		try {
			String indexName = objectMessage.getStringProperty( Environment.INDEX_NAME_JMS_PROPERTY );
			IndexManager indexManager = searchIntegrator.getIndexManager( indexName );
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
