//$Id$
package org.hibernate.search.test.jms.slave;

import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;

import org.apache.activemq.broker.BrokerService;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Checks that the Slave in a JMS configuration proplerly places index jobs onto the queue.
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
	 * Name of the connection factort as found in JNDI (see jndi.properties).
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
		Thread.sleep(500);

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 2, SearchQueueChecker.works );

		SearchQueueChecker.reset();
		s = openSession();
		tx = s.beginTransaction();
		ts = (TShirt) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep(500);

		assertEquals( 1, SearchQueueChecker.queues );
		assertEquals( 2, SearchQueueChecker.works ); //one update = 2 works

		SearchQueueChecker.reset();
		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( TShirt.class, ts.getId() ) );
		s.delete( s.get( TShirt.class, ts2.getId() ) );
		tx.commit();

		//Need to sleep for the message consumption
		Thread.sleep(500);
		
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
		Context ctx = new javax.naming.InitialContext();
		return ( Queue ) ctx.lookup( QUEUE_NAME );
	}

	private QueueSession getQueueSession() throws Exception {
		if ( queueSession == null ) {
			Context ctx = new javax.naming.InitialContext();
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
	}

	protected Class[] getMappings() {
		return new Class[] {
				TShirt.class
		};
	}
}
