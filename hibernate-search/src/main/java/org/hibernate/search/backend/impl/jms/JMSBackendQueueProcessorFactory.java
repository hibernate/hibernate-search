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

import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.UpdatableBackendQueueProcessorFactory;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.JNDIHelper;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class JMSBackendQueueProcessorFactory implements UpdatableBackendQueueProcessorFactory {
	private String jmsQueueName;
	private String jmsConnectionFactoryName;
	private static final String JNDI_PREFIX = Environment.WORKER_PREFIX + "jndi.";
	private Properties properties;
	private Queue jmsQueue;
	private QueueConnectionFactory factory;
	public static final String JMS_CONNECTION_FACTORY = Environment.WORKER_PREFIX + "jms.connection_factory";
	public static final String JMS_QUEUE = Environment.WORKER_PREFIX + "jms.queue";

	public void initialize(Properties props, WorkerBuildContext context) {
		//TODO proper exception if jms queues and connections are not there
		this.properties = props;
		this.jmsConnectionFactoryName = props.getProperty( JMS_CONNECTION_FACTORY );
		this.jmsQueueName = props.getProperty( JMS_QUEUE );
		prepareJMSTools();
	}

	public void updateDirectoryProviders(Set<DirectoryProvider<?>> providers, WorkerBuildContext context) {
		//nothing to do here, this backend is not sensible to directory providers
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new JMSBackendQueueProcessor( queue, this );
	}


	public QueueConnectionFactory getJMSFactory() {
		return factory;
	}

	public Queue getJmsQueue() {
		return jmsQueue;
	}


	public String getJmsQueueName() {
		return jmsQueueName;
	}

	public void prepareJMSTools() {
		if ( jmsQueue != null && factory != null ) {
			return;
		}
		try {
			InitialContext initialContext = JNDIHelper.getInitialContext( properties, JNDI_PREFIX );
			factory = ( QueueConnectionFactory ) initialContext.lookup( jmsConnectionFactoryName );
			jmsQueue = ( Queue ) initialContext.lookup( jmsQueueName );

		}
		catch ( NamingException e ) {
			throw new SearchException(
					"Unable to lookup Search queue ("
							+ ( jmsQueueName != null ?
							jmsQueueName :
							"null" ) + ") and connection factory ("
							+ ( jmsConnectionFactoryName != null ?
							jmsConnectionFactoryName :
							"null" ) + ")",
					e
			);
		}
	}

	public void close() {
		// no need to release anything
	}
}
