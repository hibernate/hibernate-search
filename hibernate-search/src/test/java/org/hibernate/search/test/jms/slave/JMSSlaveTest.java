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

import java.util.Properties;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.activemq.broker.BrokerService;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.test.SearchTestCase;

/**
 * Checks that the Slave in a JMS configuration property places index jobs onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class JMSSlaveTest extends SearchTestCase {

	/**
	 * Name of the test queue as found in JNDI  (see jndi.properties).
	 */
	private static final String QUEUE_NAME = "queue/searchtest";

	/**
	 * Name of the connection factory as found in JNDI (see jndi.properties).
	 */
	private static final String CONNECTION_FACTORY_NAME = "java:/ConnectionFactory";

	/**
	 * ActiveMQ message broker.
	 */
	private BrokerService brokerService;

	private QueueSession queueSession;

	public void testMessageSend() throws Exception {
		registerMessageListener();
		SearchQueueChecker.reset();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		TShirt ts = new TShirt();
		ts.setLogo( "Boston" );
		ts.setSize( "XXL" );
		TShirt ts2 = new TShirt();
		ts2.setLogo( "Mapple leaves" );
		ts2.setSize( "L" );
		s.persist( ts );
		s.persist( ts2 );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep( 500 );

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 2, SearchQueueChecker.works );

		SearchQueueChecker.reset();
		s = openSession();
		tx = s.beginTransaction();
		ts = ( TShirt ) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep( 500 );

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 2, SearchQueueChecker.works ); //one update = 2 works

		SearchQueueChecker.reset();
		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( TShirt.class, ts.getId() ) );
		s.delete( s.get( TShirt.class, ts2.getId() ) );
		tx.commit();

		//Need to sleep for the message consumption
		Thread.sleep( 500 );

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 2, SearchQueueChecker.works );
		s.close();
	}

	protected void setUp() throws Exception {
		// create and start the brokerService
		brokerService = new BrokerService();
		brokerService.setPersistent( false );
		brokerService.start();

		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if ( brokerService != null ) {
			brokerService.stop();
		}
	}

	private void registerMessageListener() throws Exception {
		MessageConsumer consumer = getQueueSession().createConsumer( getMessageQueue() );
		consumer.setMessageListener( new SearchQueueChecker() );
	}

	private Queue getMessageQueue() throws Exception {
		Context ctx = getJndiInitialContext();
		return ( Queue ) ctx.lookup( QUEUE_NAME );
	}

	private QueueSession getQueueSession() throws Exception {
		if ( queueSession == null ) {
			Context ctx = getJndiInitialContext();
			QueueConnectionFactory factory = ( QueueConnectionFactory ) ctx.lookup( CONNECTION_FACTORY_NAME );
			QueueConnection conn = factory.createQueueConnection();
			conn.start();
			queueSession = conn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );

		}
		return queueSession;
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.WORKER_BACKEND, "jms" );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_CONNECTION_FACTORY, CONNECTION_FACTORY_NAME );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_QUEUE, QUEUE_NAME );

		// use the hibernate.search.worker.jndi prefix to pass a whole bunch of jndi properties to create the InitialContext
		// for the queue processor
		cfg.setProperty(
				"hibernate.search.worker.jndi.class", "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
		);
		cfg.setProperty( "hibernate.search.worker.jndi.url", "vm://localhost" );
		cfg.setProperty( "hibernate.search.worker.jndi.connectionFactoryNames", "ConnectionFactory, java:/ConnectionFactory" );
		cfg.setProperty( "hibernate.search.worker.jndi.queue.queue/searchtest", "searchQueue" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	private Context getJndiInitialContext() throws NamingException {
		Properties props = new Properties();
		props.setProperty(
				Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
		);
		props.setProperty( Context.PROVIDER_URL, "vm://localhost" );
		props.setProperty( "connectionFactoryNames", "ConnectionFactory, java:/ConnectionFactory" );
		props.setProperty( "queue.queue/searchtest", "searchQueue" );

		Context ctx = new javax.naming.InitialContext( props );
		return ctx;
	}
}
