/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.master;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.MessageSerializationHelper;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that the Master node in a JGroups cluster can properly process messages received from channel.
 * <p>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class JGroupsMasterTest extends SearchTestBase {

	private static final Poller POLLER = JGroupsCommonTest.POLLER;
	private static final IndexedTypeIdentifier tshirtType = new PojoIndexedTypeIdentifier( TShirt.class );

	private final QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	private JChannel channel;

	@Test
	public void testMessageSending() throws Exception {
		TShirt shirt = createObject();
		List<LuceneWork> queue = createDocumentAndWorkQueue( shirt );

		assertEquals( 0, countByQuery( "logo:jboss" ) );
		sendMessage( queue );

		POLLER.pollAssertion( () -> {
			Assert.assertEquals( "Message not received after waiting for long!",
					1, countByQuery( "logo:jboss" ) );
		} );
	}

	private int countByQuery(String luceneQueryString) throws ParseException {
		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		try {
			ftSess.getTransaction().begin();
			try {
				Query luceneQuery = parser.parse( luceneQueryString );
				FullTextQuery query = ftSess.createFullTextQuery( luceneQuery );
				List result = query.list();
				return result.size();
			}
			finally {
				ftSess.getTransaction().commit();
			}
		}
		finally {
			ftSess.close();
		}
	}

	private void prepareJGroupsChannel() throws Exception {
		channel = new JChannel( ConfigurationParseHelper.locateConfig( "testing-flush-loopback.xml" ) );
		channel.connect( CHANNEL_NAME );
	}

	private void sendMessage(List<LuceneWork> queue) throws Exception {
		final String indexManagerName = getIndexName();
		ServiceManager serviceManager = getExtendedSearchIntegrator().getServiceManager();

		//send message to all listeners
		byte[] data = serviceManager.requestService( LuceneWorkSerializer.class ).toSerializedModel( queue );
		data = MessageSerializationHelper.prependString( indexManagerName, data );
		Message message = new Message( null, null, data );
		channel.send( message );

		serviceManager.releaseService( LuceneWorkSerializer.class );
	}

	protected String getIndexName() {
		return tshirtType.getName();
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
		field = new Field( "id", String.valueOf( shirt.getId() ), Field.Store.YES, Field.Index.NOT_ANALYZED );
		doc.add( field );
		field = new Field( "logo", shirt.getLogo(), Field.Store.NO, Field.Index.ANALYZED );
		doc.add( field );
		DoubleField numField = new DoubleField( "length", shirt.getLength(), Field.Store.NO );
		doc.add( numField );
		LuceneWork luceneWork = new AddLuceneWork(
				shirt.getId(), String.valueOf( shirt.getId() ), tshirtType, doc
		);
		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( luceneWork );
		return queue;
	}

	/**
	 * Create a test object without triggering indexing,
	 * because Hibernate Search listeners are disabled.
	 *
	 * @return a <code>TShirt</code> test object.
	 */
	private TShirt createObject() {
		Session s = openSession();
		s.getTransaction().begin();
		TShirt ts = new TShirt();
		ts.setLogo( "JBoss balls" );
		ts.setSize( "large" );
		ts.setLength( 23.2d );
		s.persist( ts );
		s.getTransaction().commit();
		s.close();
		return ts;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		prepareJGroupsChannel();
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		channel.close();
		super.tearDown();
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		// See createObject()
		cfg.put( Environment.INDEXING_STRATEGY, IndexingMode.MANUAL.toExternalRepresentation() );

		// JGroups configuration for master node
		cfg.put( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.put( DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.put( DispatchMessageSender.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}
}
