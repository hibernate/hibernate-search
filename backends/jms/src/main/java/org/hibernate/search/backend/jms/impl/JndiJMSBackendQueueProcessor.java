/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jms.impl;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import org.hibernate.search.engine.service.named.spi.NamedResolver;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * JMSBackendQueueProcessor using JNDI to lookup the JMS components.
 */
public class JndiJMSBackendQueueProcessor extends JmsBackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	@Override
	protected QueueConnectionFactory initializeJMSQueueConnectionFactory(Properties properties) {
		final String jmsConnectionFactoryName = properties.getProperty( JMS_CONNECTION_FACTORY );
		try ( ServiceReference<NamedResolver> requestReference = getServiceManager().requestReference( NamedResolver.class ) ) {
			Object located = requestReference.get().locate( jmsConnectionFactoryName );
			return (QueueConnectionFactory) located;
		}
		catch (RuntimeException e) {
			throw log.jmsQueueFactoryLookupException( jmsConnectionFactoryName, getIndexName(), e );
		}
	}

	@Override
	protected Queue initializeJMSQueue(QueueConnectionFactory factory, Properties properties) {
		try ( ServiceReference<NamedResolver> requestReference = getServiceManager().requestReference( NamedResolver.class ) ) {
			Object located = requestReference.get().locate( getJmsQueueName() );
			return (Queue) located;
		}
		catch (RuntimeException e) {
			throw log.jmsQueueLookupException( getJmsQueueName(), getIndexName(), e );
		}
	}

	@Override
	protected QueueConnection initializeJMSConnection(QueueConnectionFactory factory, Properties properties) {
		final String login = properties.getProperty( JMS_CONNECTION_LOGIN );
		final String password = properties.getProperty( JMS_CONNECTION_PASSWORD );
		try {
			if ( login == null && password == null ) {
				return factory.createQueueConnection();
			}
			else {
				return factory.createQueueConnection( login, password );
			}
		}
		catch (JMSException e) {
			throw log.unableToOpenJMSConnection( getIndexName(), getJmsQueueName(), e );
		}
	}

}
