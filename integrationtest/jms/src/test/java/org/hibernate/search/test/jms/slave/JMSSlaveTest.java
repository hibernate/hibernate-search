/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.jms.impl.JmsBackendQueueProcessor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jms.master.JMSMasterTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Checks that the Slave in a JMS configuration property places index jobs onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class JMSSlaveTest extends SearchTestBase {

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

	@Test
	public void testMessageSend() throws Exception {
		registerMessageListener();
		SearchQueueChecker.reset();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		TShirt ts = new TShirt();
		ts.setLogo( "Boston" );
		ts.setSize( "XXL" );
		ts.setLength( 23.4d );
		TShirt ts2 = new TShirt();
		ts2.setLogo( "Mapple leaves" );
		ts2.setSize( "L" );
		ts2.setLength( 23.42d );
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
		ts = (TShirt) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep( 500 );

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 1, SearchQueueChecker.works );

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

	@Override
	@Before
	public void setUp() throws Exception {
		// create and start the brokerService
		brokerService = JMSMasterTest.createTestingBrokerService();
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if ( brokerService != null ) {
			brokerService.stop();
		}
	}

	private void registerMessageListener() throws Exception {
		MessageConsumer consumer = getQueueSession().createConsumer( getMessageQueue() );
		consumer.setMessageListener( new SearchQueueChecker( getExtendedSearchIntegrator() ) );
	}

	private Queue getMessageQueue() throws Exception {
		Context ctx = getJndiInitialContext();
		return (Queue) ctx.lookup( QUEUE_NAME );
	}

	private QueueSession getQueueSession() throws Exception {
		if ( queueSession == null ) {
			Context ctx = getJndiInitialContext();
			QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup( CONNECTION_FACTORY_NAME );
			QueueConnection conn = factory.createQueueConnection();
			conn.start();
			queueSession = conn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );

		}
		return queueSession;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jms" );
		cfg.setProperty( "hibernate.search.default." + JmsBackendQueueProcessor.JMS_CONNECTION_FACTORY, CONNECTION_FACTORY_NAME );
		cfg.setProperty( "hibernate.search.default." + JmsBackendQueueProcessor.JMS_QUEUE, QUEUE_NAME );

		// use the hibernate.search.worker.jndi prefix to pass a whole bunch of jndi properties to create the InitialContext
		// for the queue processor
		cfg.setProperty(
				"hibernate.search.default.worker.jndi.class", "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
		);
		cfg.setProperty( "hibernate.search.default.worker.jndi.url", "vm://localhost" );
		cfg.setProperty( "hibernate.search.default.worker.jndi.connectionFactoryNames", "ConnectionFactory, java:/ConnectionFactory" );
		cfg.setProperty( "hibernate.search.default.worker.jndi.queue.queue/searchtest", "searchQueue" );
	}

	@Override
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
