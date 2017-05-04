/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.jms;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Example template to implement the Hibernate Search controller for processing the
 * work send through JMS by the slave nodes.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public abstract class AbstractJMSHibernateSearchController implements MessageListener {

	private static final Log log = LoggerFactory.make();

	protected abstract SearchIntegrator getSearchIntegrator();

	/**
	 * Provides an optional extension point for the case you have to
	 * do some cleanup after the Message was processed.
	 *
	 * This is invoked once by {@link #onMessage(Message)} after processing
	 * each Message, provided the type of the Message is {@link ObjectMessage}
	 * as expected.
	 */
	protected void afterMessage() {
		// No-op by default: you can override this if needed.
	}

	/**
	 * Process the Hibernate Search work queues received
	 */
	@Override
	public void onMessage(Message message) {
		if ( !( message instanceof ObjectMessage ) ) {
			log.incorrectMessageType( message.getClass() );
			return;
		}
		final ObjectMessage objectMessage = (ObjectMessage) message;
		final String indexName;
		final List<LuceneWork> queue;
		SearchIntegrator integrator = getSearchIntegrator();
		try {
			indexName = extractIndexName( objectMessage );
			if ( log.isDebugEnabled() ) {
				logMessageDetails( objectMessage, indexName );
			}

			queue = integrator.getWorkSerializer().toLuceneWorks( (byte[]) objectMessage.getObject() );

			/*
			 * We have to use a dispatcher here in order to make sure
			 * the sharding strategies lazily initialize new indexes as necessary
			 * and update their shard list.
			 * Thus the index name is rather useless in this case.
			 */
			OperationDispatcher dispatcher = getOperationDispatcher( integrator );
			dispatcher.dispatch( queue, null );
		}
		catch (JMSException e) {
			log.unableToRetrieveObjectFromMessage( message.getClass(), e );
			return;
		}
		catch (ClassCastException e) {
			log.illegalObjectRetrievedFromMessage( e );
			return;
		}
		finally {
			afterMessage();
		}
	}

	private OperationDispatcher getOperationDispatcher(SearchIntegrator integrator) {
		return integrator.createRemoteOperationDispatcher( indexManager -> true );
	}

	private void logMessageDetails(ObjectMessage objectMessage, String indexName) throws JMSException {
		String id = objectMessage.getStringProperty( "HSearchMsgId" );
		log.debug( "Message Received for index '" + indexName + "': " + id );
	}

	private String extractIndexName(ObjectMessage objectMessage) throws JMSException {
		String name = objectMessage.getStringProperty( Environment.INDEX_NAME_JMS_PROPERTY );
		if ( name == null ) {
			//Fall back to try the property name we used before HSEARCH-1922
			name = objectMessage.getStringProperty( "HSearchIndexName" );
		}
		return name;
	}

}
