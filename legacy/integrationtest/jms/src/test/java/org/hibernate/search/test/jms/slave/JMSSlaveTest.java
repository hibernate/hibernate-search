/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jms.slave;

import java.util.Map;
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
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.backend.jms.impl.JmsBackendQueueProcessor;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jms.master.JMSMasterTest;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
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

	private static final IndexedTypeIdentifier TSHIRT_TYPE = new PojoIndexedTypeIdentifier( TShirt.class );

	/**
	 * Name of the test queue as found in JNDI  (see jndi.properties).
	 */
	private static final String QUEUE_NAME = "queue/searchtest";

	/**
	 * Name of the connection factory as found in JNDI (see jndi.properties).
	 */
	private static final String CONNECTION_FACTORY_NAME = "java:/ConnectionFactory";

	private static final Poller POLLER = Poller.milliseconds( 1_000, 200 );

	/**
	 * ActiveMQ message broker.
	 */
	private BrokerService brokerService;

	private QueueSession queueSession;

	@Test
	public void testMessageSend() throws Exception {
		registerMessageListener();
		SearchQueueChecker.reset();

		TShirt ts;
		TShirt ts2;

		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			ts = new TShirt();
			ts.setLogo( "Boston" );
			ts.setSize( "XXL" );
			ts.setLength( 23.4d );
			ts2 = new TShirt();
			ts2.setLogo( "Mapple leaves" );
			ts2.setSize( "L" );
			ts2.setLength( 23.42d );
			s.persist( ts );
			s.persist( ts2 );
			tx.commit();
		}

		//need to sleep for the message consumption
		POLLER.pollAssertion( () -> {
			assertEquals( 1, SearchQueueChecker.queues );
			assertEquals( 2, SearchQueueChecker.works );
		} );

		SearchQueueChecker.reset();
		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			ts = (TShirt) s.get( TShirt.class, ts.getId() );
			ts.setLogo( "Peter pan" );
			tx.commit();
		}

		//need to sleep for the message consumption
		POLLER.pollAssertion( () -> {
			assertEquals( 1, SearchQueueChecker.queues );
			assertEquals( 1, SearchQueueChecker.works );
		} );

		SearchQueueChecker.reset();

		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			s.delete( s.get( TShirt.class, ts.getId() ) );
			s.delete( s.get( TShirt.class, ts2.getId() ) );
			tx.commit();
		}

		//Need to sleep for the message consumption
		POLLER.pollAssertion( () -> {
			assertEquals( 1, SearchQueueChecker.queues );
			assertEquals( 2, SearchQueueChecker.works );
		} );

		SearchQueueChecker.reset();
		try ( Session s = openSession() ) {
			Transaction tx = s.beginTransaction();
			TransactionContextForTest tc = new TransactionContextForTest();
			ExtendedSearchIntegrator integrator = this.getExtendedSearchIntegrator();
			integrator.getWorker().performWork( new DeleteByQueryWork( TSHIRT_TYPE, new SingularTermDeletionQuery( "id", String.valueOf( ts.getId() ) ) ), tc );
			tc.end();
			tx.commit();
		}

		// Need to sleep for the message consumption
		POLLER.pollAssertion( () -> {
			assertEquals( 1, SearchQueueChecker.queues );
			assertEquals( 1, SearchQueueChecker.works );
		} );
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
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jms" );
		cfg.put( "hibernate.search.default." + JmsBackendQueueProcessor.JMS_CONNECTION_FACTORY, CONNECTION_FACTORY_NAME );
		cfg.put( "hibernate.search.default." + JmsBackendQueueProcessor.JMS_QUEUE, QUEUE_NAME );

		// use the hibernate.jndi prefix to pass a whole bunch of jndi properties to create the InitialContext
		// for the queue processor
		cfg.put( "hibernate.jndi.class", "org.apache.activemq.jndi.ActiveMQInitialContextFactory" );
		cfg.put( "hibernate.jndi.url", "vm://localhost" );
		cfg.put( "hibernate.jndi.connectionFactoryNames", "ConnectionFactory, java:/ConnectionFactory" );
		cfg.put( "hibernate.jndi.queue.queue/searchtest", "searchQueue" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
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
