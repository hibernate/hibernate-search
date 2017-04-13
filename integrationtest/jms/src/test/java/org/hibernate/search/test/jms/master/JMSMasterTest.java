/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jms.master;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import javax.persistence.criteria.CriteriaDelete;

import org.apache.activemq.broker.BrokerService;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the Master node in a JMS cluster can properly process messages placed onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class JMSMasterTest extends SearchTestBase {

	/**
	 * Name of the test queue as found in JNDI (see jndi.properties).
	 */
	private static final String QUEUE_NAME = "queue/searchtest";

	/**
	 * Name of the connection factory as found in JNDI (see jndi.properties).
	 */
	private static final String CONNECTION_FACTORY_NAME = "java:/ConnectionFactory";

	public static final Poller POLLER = Poller.milliseconds( 10_000, 100 );

	private final QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );

	/**
	 * ActiveMQ message broker.
	 */
	private BrokerService brokerService;

	private QueueSession queueSession;

	@Test
	public void testMessageSending() throws Exception {

		TShirt shirt = createObjectWithSQL();
		List<LuceneWork> queue = createDocumentAndWorkQueue( shirt );

		registerMessageListener();

		assertEquals( 0, listByQuery( "logo:jboss" ).size() );

		sendMessage( queue );

		// need to sleep to give JMS processing and indexing time
		POLLER.pollAssertion( () -> assertEquals( 1, listByQuery( "logo:jboss" ).size() ) );

		FullTextSession ftSession = Search.getFullTextSession( openSession() );
		ftSession.getTransaction().begin();
		CriteriaDelete<TShirt> delete = ftSession.getCriteriaBuilder().createCriteriaDelete( TShirt.class );
		delete.from( TShirt.class );
		ftSession.createQuery( delete ).executeUpdate();
		ftSession.purgeAll( TShirt.class );
		ftSession.getTransaction().commit();
		ftSession.close();

		assertEquals( 0, listByQuery( "logo:jboss" ).size() );

		{
			shirt = createObjectWithSQL();
			queue = createDocumentAndWorkQueue( shirt );

			registerMessageListener();
			sendMessage( queue );

			POLLER.pollAssertion( () -> assertEquals( 1, listByQuery( "logo:jboss" ).size() ) );

			{
				DeleteByQueryLuceneWork work = new DeleteByQueryLuceneWork( TShirt.class, new SingularTermDeletionQuery( "logo", "jboss" ) );
				List<LuceneWork> l = new ArrayList<>();
				l.add( work );
				this.registerMessageListener();
				this.sendMessage( l );
			}

			POLLER.pollAssertion( () -> {
				HSQuery hsQuery = this.getExtendedSearchIntegrator().createHSQuery(
						this.getExtendedSearchIntegrator().buildQueryBuilder().forEntity( TShirt.class ).get().all().createQuery(),
						TShirt.class
						);
				assertEquals( 0, hsQuery.queryResultSize() );
			} );
		}
	}

	@SuppressWarnings("unchecked")
	private List<TShirt> listByQuery(String luceneQueryString) throws ParseException {
		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		try {
			ftSess.getTransaction().begin();
			try {
				Query luceneQuery = parser.parse( luceneQueryString );
				FullTextQuery query = ftSess.createFullTextQuery( luceneQuery );
				@SuppressWarnings({ "rawtypes" })
				List result = query.list();
				return result;
			}
			finally {
				ftSess.getTransaction().commit();
			}
		}
		finally {
			ftSess.close();
		}
	}

	private void registerMessageListener() throws Exception {
		MessageConsumer consumer = getQueueSession().createConsumer( getMessageQueue() );
		consumer.setMessageListener( new MDBSearchController( getExtendedSearchIntegrator() ) );
	}

	private void sendMessage(List<LuceneWork> queue) throws Exception {
		ObjectMessage message = getQueueSession().createObjectMessage();
		final String indexName = org.hibernate.search.test.jms.master.TShirt.class.getName();
		message.setStringProperty(
				Environment.INDEX_NAME_JMS_PROPERTY,
				indexName );
		IndexManager indexManager = getExtendedSearchIntegrator().getIndexManagerHolder().getIndexManager( indexName );
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
		field = new Field( "id", "1", Field.Store.YES, Field.Index.ANALYZED );
		doc.add( field );
		field = new Field( "logo", shirt.getLogo(), Field.Store.NO, Field.Index.ANALYZED );
		doc.add( field );
		DoubleField numField = new DoubleField( "length", shirt.getLength(), Field.Store.NO );
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
		TShirt ts = s.get( TShirt.class, 1 );
		s.getTransaction().commit();
		s.close();
		return ts;
	}

	@Override
	@Before
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
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if ( brokerService != null ) {
			brokerService.stop();
		}
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		// explicitly set the backend even though local is default.
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "local" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}
}
