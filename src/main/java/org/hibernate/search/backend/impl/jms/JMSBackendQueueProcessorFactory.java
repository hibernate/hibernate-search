//$Id$
package org.hibernate.search.backend.impl.jms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;

/**
 * @author Emmanuel Bernard
 */
public class JMSBackendQueueProcessorFactory implements BackendQueueProcessorFactory {
	private String jmsQueueName;
	private String jmsConnectionFactoryName;
	private static final String JNDI_PREFIX = Environment.WORKER_PREFIX + "jndi.";
	private Properties properties;
	private Queue jmsQueue;
	private QueueConnectionFactory factory;
	public static final String JMS_CONNECTION_FACTORY = Environment.WORKER_PREFIX + "jms.connection_factory";
	public static final String JMS_QUEUE = Environment.WORKER_PREFIX + "jms.queue";

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		//TODO proper exception if jms queues and connecitons are not there
		this.properties = props;
		this.jmsConnectionFactoryName = props.getProperty( JMS_CONNECTION_FACTORY );
		this.jmsQueueName = props.getProperty( JMS_QUEUE );
		prepareJMSTools();
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
		if ( jmsQueue != null && factory != null ) return;
		try {
			InitialContext initialContext = getInitialContext( properties );
			factory = (QueueConnectionFactory) initialContext.lookup( jmsConnectionFactoryName );
			jmsQueue = (Queue) initialContext.lookup( jmsQueueName );

		}
		catch (NamingException e) {
			throw new SearchException( "Unable to lookup Search queue ("
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

	private InitialContext getInitialContext(Properties properties) throws NamingException {
		Properties jndiProps = getJndiProperties( properties );
		if ( jndiProps.size() == 0 ) {
			return new InitialContext();
		}
		else {
			return new InitialContext( jndiProps );
		}
	}

	private static Properties getJndiProperties(Properties properties) {

		HashSet specialProps = new HashSet();
		specialProps.add( JNDI_PREFIX + "class" );
		specialProps.add( JNDI_PREFIX + "url" );

		Iterator iter = properties.keySet().iterator();
		Properties result = new Properties();
		while ( iter.hasNext() ) {
			String prop = (String) iter.next();
			if ( prop.indexOf( JNDI_PREFIX ) > -1 && !specialProps.contains( prop ) ) {
				result.setProperty(
						prop.substring( JNDI_PREFIX.length() ),
						properties.getProperty( prop )
				);
			}
		}

		String jndiClass = properties.getProperty( JNDI_PREFIX + "class" );
		String jndiURL = properties.getProperty( JNDI_PREFIX + "url" );
		// we want to be able to just use the defaults,
		// if JNDI environment properties are not supplied
		// so don't put null in anywhere
		if ( jndiClass != null ) result.put( Context.INITIAL_CONTEXT_FACTORY, jndiClass );
		if ( jndiURL != null ) result.put( Context.PROVIDER_URL, jndiURL );

		return result;
	}

	public void close() {
		// no need to release anything
	}

}
