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

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import org.hibernate.search.Environment;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public abstract class JmsBackendQueueProcessor implements BackendQueueProcessor {

	private String jmsQueueName;
	protected static final String JNDI_PREFIX = Environment.WORKER_PREFIX + "jndi.";
	private Queue jmsQueue;
	private String indexName;
	private SearchFactoryImplementor searchFactory;
	private QueueConnection connection;

	public static final String JMS_CONNECTION_FACTORY = Environment.WORKER_PREFIX + "jms.connection_factory";
	public static final String JMS_QUEUE = Environment.WORKER_PREFIX + "jms.queue";
	public static final String JMS_CONNECTION_LOGIN = Environment.WORKER_PREFIX + "jms.login";
	public static final String JMS_CONNECTION_PASSWORD = Environment.WORKER_PREFIX + "jms.password";

	private IndexManager indexManager;

	private static final Log log = LoggerFactory.make();

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		this.indexManager = indexManager;
		this.jmsQueueName = props.getProperty( JMS_QUEUE );
		this.indexName = indexManager.getIndexName();
		this.searchFactory = context.getUninitializedSearchFactory();
		QueueConnectionFactory factory = initializeJMSQueueConnectionFactory( props );
		this.jmsQueue = initializeJMSQueue( factory, props );
		this.connection = initializeJMSConnection( factory, props );
	}

	public Queue getJmsQueue() {
		return jmsQueue;
	}

	public String getJmsQueueName() {
		return jmsQueueName;
	}

	public String getIndexName() {
		return indexName;
	}

	public SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( workList == null ) {
			throw new IllegalArgumentException( "workList should not be null" );
		}

		Runnable operation = new JmsBackendQueueTask( indexName, workList, indexManager, this );
		operation.run();
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		applyWork( Collections.singletonList( singleOperation ), monitor );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		log.warnSuspiciousBackendDirectoryCombination( indexName );
		return new ReentrantLock(); // keep the invoker happy, still it's useless
	}

	@Override
	public void indexMappingChanged() {
		// no-op
	}

	public QueueConnection getJMSConnection() {
		return connection;
	}

	@Override
	public void close() {
		try {
			if ( connection != null ) {
				connection.close();
			}
		}
		catch (JMSException e) {
			log.unableToCloseJmsConnection( jmsQueueName, e );
		}
	}

	/**
	 * Initialises the JMS QueueConnectionFactory to be used for sending Lucene work operations to the master node.
	 *
	 * @return the initialized {@link javax.jms.QueueConnectionFactory}
	 * @param props a {@link java.util.Properties} object.
	 */
	protected abstract QueueConnectionFactory initializeJMSQueueConnectionFactory(Properties props);

	/**
	 * Initialises the JMS queue to be used for sending Lucene work operations to the master node.
	 * Invoked after {@link #initializeJMSQueueConnectionFactory(Properties)}
	 *
	 * @return the initialized {@link javax.jms.Queue}
	 * @param factory a {@link javax.jms.QueueConnectionFactory} object.
	 * @param props a {@link java.util.Properties} object.
	 */
	protected abstract Queue initializeJMSQueue(QueueConnectionFactory factory, Properties props);

	/**
	 * Initialises the JMS QueueConnection to be used for sending Lucene work operations to the master node.
	 * This is invoked after {@link #initializeJMSQueue(QueueConnectionFactory, Properties)}.
	 *
	 * @return the initialized {@link javax.jms.QueueConnection}
	 * @param factory a {@link javax.jms.QueueConnectionFactory} object.
	 * @param props a {@link java.util.Properties} object.
	 */
	protected abstract QueueConnection initializeJMSConnection(QueueConnectionFactory factory, Properties props);

}
