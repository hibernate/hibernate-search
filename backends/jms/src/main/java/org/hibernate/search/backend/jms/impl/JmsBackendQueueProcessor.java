/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jms.impl;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Backend sending the workload via JMS.
 * Optionally support transactions: the JMS messages can be sent as part of the current transaction.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Yoann Gendre
 */
public abstract class JmsBackendQueueProcessor implements BackendQueueProcessor, BackendQueueProcessor.Transactional {

	private String jmsQueueName;
	protected static final String JNDI_PREFIX = Environment.WORKER_PREFIX + "jndi.";
	private Queue jmsQueue;
	private QueueConnectionFactory factory;
	private String indexName;
	private SearchIntegrator integrator;
	private QueueConnection connection;

	public static final String JMS_CONNECTION_FACTORY = Environment.WORKER_PREFIX + "jms.connection_factory";
	public static final String JMS_QUEUE = Environment.WORKER_PREFIX + "jms.queue";
	public static final String JMS_CONNECTION_LOGIN = Environment.WORKER_PREFIX + "jms.login";
	public static final String JMS_CONNECTION_PASSWORD = Environment.WORKER_PREFIX + "jms.password";

	private static final Log log = LoggerFactory.make();

	private Properties props = null;
	private boolean isTransactional;
	private ServiceManager serviceManager;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		serviceManager = context.getServiceManager();
		this.props = props;
		this.isTransactional = context.enlistWorkerInTransaction();
		this.jmsQueueName = props.getProperty( JMS_QUEUE );
		this.indexName = indexManager.getIndexName();
		this.integrator = context.getUninitializedSearchIntegrator();
		this.factory = initializeJMSQueueConnectionFactory( props );

		if ( ! isTransactional ) {
			// if we are not transactional, we can eagerly initialize the queue and connection
			this.jmsQueue = initializeJMSQueue( factory, props );
			this.connection = initializeJMSConnection( factory, props );
		}
	}

	public Queue getJmsQueue() {
		if ( isTransactional ) {
			// return jmsQueue;
			return initializeJMSQueue( factory, props );
		}
		else {
			return jmsQueue;
		}
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( workList == null ) {
			throw new IllegalArgumentException( "workList should not be null" );
		}

		Runnable operation = new JmsBackendQueueTask( indexName, workList, this, integrator.getWorkSerializer() );
		operation.run();
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		applyWork( Collections.singletonList( singleOperation ), monitor );
	}

	public QueueConnection getJMSConnection() {
		// return connection;
		if ( isTransactional ) {
			return initializeJMSConnection( factory, props );
		}
		else {
			return connection;
		}
	}

	public void releaseJMSConnection(QueueConnection queueConnection) {
		if ( isTransactional ) {
			// we create the connection each time
			// let's close it
			try {
				if ( queueConnection != null ) {
					queueConnection.close();
				}
			}
			catch (JMSException e) {
				log.unableToCloseJmsConnection( jmsQueueName, e );
			}
		}
		else {
			// we share the connection accross calls
			// noop
		}
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

	public final String getJmsQueueName() {
		return jmsQueueName;
	}

	public final String getIndexName() {
		return indexName;
	}

	public final SearchIntegrator getSearchIntegrator() {
		return integrator;
	}

	public final boolean isTransactional() {
		return isTransactional;
	}

	public final ServiceManager getServiceManager() {
		return serviceManager;
	}

}
