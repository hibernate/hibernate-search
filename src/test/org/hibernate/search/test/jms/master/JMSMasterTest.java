//$Id$
package org.hibernate.search.test.jms.master;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.test.SearchTestCase;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.embedded.Bootstrap;

/**
 * @author Emmanuel Bernard
 */
public class JMSMasterTest extends SearchTestCase {

	private Bootstrap bootstrap;

	public void testMessageSending() throws Exception {
		MyHibernateUtil.sessionFactory = getSessions();

		//create an object wo trigggering indexing
		Session s = openSession( );
		s.getTransaction().begin();
		s.connection().createStatement().executeUpdate(
				"insert into TShirt_Master(id, logo, size) values( '1', 'JBoss balls', 'large')"
		);
		TShirt ts = (TShirt) s.get(TShirt.class, 1);
		s.getTransaction().commit();
		s.close();
		//create the work queue to send
		Document doc = new Document();
		Field field = new Field( DocumentBuilder.CLASS_FIELDNAME, ts.getClass().getName(), Field.Store.YES, Field.Index.UN_TOKENIZED );
		doc.add( field );
		field = new Field("id", "1", Field.Store.YES, Field.Index.UN_TOKENIZED );
		doc.add( field );
		field = new Field("logo", ts.getLogo(), Field.Store.NO, Field.Index.TOKENIZED );
		doc.add( field );
		LuceneWork luceneWork = new AddLuceneWork(ts.getId(), String.valueOf( ts.getId() ), ts.getClass(), doc );
		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( luceneWork );

		//send the queue
		InitialContext context = new InitialContext();
		QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup( "java:/ConnectionFactory" );
		Queue jmsQueue = (Queue) context.lookup( "queue/searchtest" );
		QueueConnection cnn;
		QueueSender sender;
		QueueSession session;
		cnn = factory.createQueueConnection();
		//TODO make transacted parameterized
		session = cnn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );

		ObjectMessage message = session.createObjectMessage();
		message.setObject( (Serializable) queue );

		sender = session.createSender( jmsQueue );
		sender.send( message );

		session.close();
		cnn.close();

		//wait for the message to be processed
		Thread.sleep( 1000 );

		FullTextSession ftSess = Search.createFullTextSession( openSession( ) );
		ftSess.getTransaction().begin();
		QueryParser parser = new QueryParser( "id", new StopAnalyzer() );
		Query luceneQuery = parser.parse( "logo:jboss" );
		org.hibernate.Query query = ftSess.createFullTextQuery( luceneQuery );
		List result = query.list();
		assertEquals( 1, result.size() );
		ftSess.delete( result.get( 0 ) );
		ftSess.getTransaction().commit();
		ftSess.close();
	}

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

	public static Bootstrap startupEmbeddedJBoss() {
		try {
			long start = System.currentTimeMillis();
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.bootstrap();
			bootstrap.deployResource( "jars/jms-master.jar" );
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
		cfg.setProperty( Environment.WORKER_BACKEND, "lucene" );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_CONNECTION_FACTORY, "java:/ConnectionFactory" );
		cfg.setProperty( JMSBackendQueueProcessorFactory.JMS_QUEUE, "queue/searchtest" );

	}

	protected Class[] getMappings() {
		return new Class[] {
				TShirt.class
		};
	}
}
