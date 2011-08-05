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
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessorFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.impl.JNDIHelper;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JMSBackendQueueProcessorFactory implements BackendQueueProcessorFactory {
	private String jmsQueueName;
	private String jmsConnectionFactoryName;
	private static final String JNDI_PREFIX = Environment.WORKER_PREFIX + "jndi.";
	private Properties properties;
	private Queue jmsQueue;
	private QueueConnectionFactory factory;
	private String indexName;
	private SearchFactoryImplementor searchFactory;
	public static final String JMS_CONNECTION_FACTORY = Environment.WORKER_PREFIX + "jms.connection_factory";
	public static final String JMS_QUEUE = Environment.WORKER_PREFIX + "jms.queue";

	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		//TODO proper exception if jms queues and connections are not there
		this.properties = props;
		this.jmsConnectionFactoryName = props.getProperty( JMS_CONNECTION_FACTORY );
		this.jmsQueueName = props.getProperty( JMS_QUEUE );
		this.indexName = indexManager.getIndexName();
		this.searchFactory = context.getUninitializedSearchFactory();
		prepareJMSTools();
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new JMSBackendQueueProcessor( indexName, queue, this );
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

	public SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
	}

	public void close() {
		// no need to release anything
	}
}
