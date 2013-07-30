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
package org.hibernate.search.test.jms.master;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.activemq.broker.BrokerService;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.JmsBackendQueueTask;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * Tests  that the Master node in a JMS cluster can properly process messages placed onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class JMSMasterTest extends SearchTestCase {

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

	public void testMessageSending() throws Exception {

		TShirt shirt = createObjectWithSQL();
		List<LuceneWork> queue = createDocumentAndWorkQueue( shirt );

		registerMessageListener();
		sendMessage( queue );

		// need to sleep to give JMS processing and indexing time
		Thread.sleep( 1000 );

		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		ftSess.getTransaction().begin();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );
		Query luceneQuery = parser.parse( "logo:jboss" );
		org.hibernate.Query query = ftSess.createFullTextQuery( luceneQuery );
		List result = query.list();
		assertEquals( 1, result.size() );
		ftSess.delete( result.get( 0 ) );
		ftSess.getTransaction().commit();
		ftSess.close();
	}

	private void registerMessageListener() throws Exception {
		MessageConsumer consumer = getQueueSession().createConsumer( getMessageQueue() );
		consumer.setMessageListener( new MDBSearchController( getSessionFactory() ) );
	}

	private void sendMessage(List<LuceneWork> queue) throws Exception {
		ObjectMessage message = getQueueSession().createObjectMessage();
		final String indexName = org.hibernate.search.test.jms.master.TShirt.class.getName();
		message.setStringProperty(
				JmsBackendQueueTask.INDEX_NAME_JMS_PROPERTY,
				indexName );
		IndexManager indexManager = getSearchFactoryImpl().getIndexManagerHolder().getIndexManager( indexName );
		byte[] data = indexManager.getSerializer().toSerializedModel( queue );
		message.setObject( data );
		QueueSender sender = getQueueSession().createSender( getMessageQueue() );
		sender.send( message );
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

	private Context getJndiInitialContext() throws NamingException {
		Properties props = new Properties();
		props.setProperty(
				Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
		);
		props.setProperty( Context.PROVIDER_URL, "vm://localhost" );
		props.setProperty( "connectionFactoryNames", "ConnectionFactory, java:/ConnectionFactory" );
		props.setProperty( "queue.queue/searchtest", "searchQueue" );
		return new javax.naming.InitialContext( props );
	}

	/**
	 * Manually create the work queue. This lists gets send by the Slaves to the Master for indexing.
	 *
	 * @param shirt The shirt to index
	 *
	 * @return A manually create <code>LuceneWork</code> list.
	 */
	private List<LuceneWork> createDocumentAndWorkQueue(TShirt shirt) {
		Document doc = new Document();
		Field field = new Field(
				ProjectionConstants.OBJECT_CLASS, shirt.getClass().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED
		);
		doc.add( field );
		field = new Field( "id", "1", Field.Store.YES, Field.Index.NOT_ANALYZED );
		doc.add( field );
		field = new Field( "logo", shirt.getLogo(), Field.Store.NO, Field.Index.ANALYZED );
		doc.add( field );
		NumericField numField = new NumericField( "length" );
		numField.setDoubleValue( shirt.getLength() );
		doc.add( numField );
		LuceneWork luceneWork = new AddLuceneWork(
				shirt.getId(), String.valueOf( shirt.getId() ), shirt.getClass(), doc
		);
		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( luceneWork );
		return queue;
	}

	/**
	 * Create a test object without triggering indexing. Use SQL directly.
	 *
	 * @return a <code>TShirt</code> test object.
	 *
	 * @throws SQLException in case the insert fails.
	 */
	private TShirt createObjectWithSQL() throws SQLException {
		Session s = openSession();
		s.getTransaction().begin();
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				final Statement statement = connection.createStatement();
				statement.executeUpdate(
						"insert into TShirt_Master(id, logo, size_, length_) values( 1, 'JBoss balls', 'large', 23.2)"
				);
				statement.close();
			}
		} );
		TShirt ts = (TShirt) s.get( TShirt.class, 1 );
		s.getTransaction().commit();
		s.close();
		return ts;
	}

	@Override
	public void setUp() throws Exception {
		// create and start the brokerService
		brokerService = createTestingBrokerService();
		super.setUp();
	}

	/**
	 * @return A started JMS Broker
	 */
	public static BrokerService createTestingBrokerService() throws Exception {
		BrokerService brokerService = new BrokerService();
		brokerService.setPersistent( false );
		// disabling the following greatly speedups the tests:
		brokerService.setUseJmx( false );
		brokerService.setUseShutdownHook( false );
		brokerService.setEnableStatistics( false );
		brokerService.start();
		return brokerService;
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		if ( brokerService != null ) {
			brokerService.stop();
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// explicitly set the backend even though lucene is default.
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "lucene" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}
}
