//$Id$
package org.hibernate.search.test.jms.slave;

import org.jboss.embedded.Bootstrap;
import org.jboss.deployers.spi.DeploymentException;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * @author Emmanuel Bernard
 */
public class JMSSlaveTest extends SearchTestCase {

	private Bootstrap bootstrap;


	protected void setUp() throws Exception {
		bootstrap = startupEmbeddedJBoss();
		try {
			super.setUp();
		}
		catch( RuntimeException e ) {
			try {
				shutdownEmbeddedJBoss(bootstrap);
			}
			catch( Exception ee ) {
				//swallow
			}
			throw e;
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		shutdownEmbeddedJBoss(bootstrap);
	}

	public void testMessageSend() throws Exception {
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

	public static Bootstrap startupEmbeddedJBoss() {
		try {
			long start = System.currentTimeMillis();
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.bootstrap();
			bootstrap.deployResource( "jars/jms-slave.jar" );
			System.out.println("JBoss Embedded boot time: " + (System.currentTimeMillis() - start) + " ms");
			return bootstrap;
		}
		catch (DeploymentException e) {
			throw new RuntimeException( "Failed to bootstrap", e );
		}
	}

	public static void shutdownEmbeddedJBoss(Bootstrap bootstrap) {
		bootstrap.shutdown();
	}


	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.WORKER_BACKEND, "jms" );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_CONNECTION_FACTORY, "java:/ConnectionFactory" );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_QUEUE, "queue/searchtest" );

	}

	protected Class[] getMappings() {
		return new Class[]{
				TShirt.class
		};
	}
}
