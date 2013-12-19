/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.search.util.impl.JNDIHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * JMSBackendQueueProcessor using JNDI to lookup the JMS components.
 */
public class JndiJMSBackendQueueProcessor extends JmsBackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private String jmsConnectionFactoryName;

	@Override
	protected QueueConnectionFactory initializeJMSQueueConnectionFactory(Properties properties) {
		this.jmsConnectionFactoryName = properties.getProperty( JMS_CONNECTION_FACTORY );
		try {
			InitialContext initialContext = JNDIHelper.getInitialContext( properties, JNDI_PREFIX );
			return (QueueConnectionFactory) initialContext.lookup( jmsConnectionFactoryName );
		}
		catch (NamingException e) {
			throw log.jmsLookupException( getJmsQueueName(), jmsConnectionFactoryName, getIndexName(), e );
		}
	}

	@Override
	protected Queue initializeJMSQueue(QueueConnectionFactory factory, Properties properties) {
		try {
			InitialContext initialContext = JNDIHelper.getInitialContext( properties, JNDI_PREFIX );
			return (Queue) initialContext.lookup( getJmsQueueName() );
		}
		catch (NamingException e) {
			throw log.jmsLookupException( getJmsQueueName(), jmsConnectionFactoryName, getIndexName(), e );
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
