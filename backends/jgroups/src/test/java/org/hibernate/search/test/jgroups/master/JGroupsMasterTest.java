/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.master;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.MessageSerializationHelper;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.testsupport.TestConstants;
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
 * <p/>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsMasterTest extends SearchTestBase {

	private final QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	private JChannel channel;

	@Test
	public void testMessageSending() throws Exception {

		assertEquals( 0, countByQuery( "logo:jboss" ) );

		TShirt shirt = createObjectWithSQL();
		List<LuceneWork> queue = createDocumentAndWorkQueue( shirt );
		sendMessage( queue );

		boolean failed = true;
		for ( int i = 0; i < JGroupsCommonTest.MAX_WAITS; i++ ) {
			Thread.sleep( JGroupsCommonTest.NETWORK_WAIT_MILLISECONDS );
			if ( countByQuery( "logo:jboss" ) == 1 ) { //the condition we're waiting for
				failed = false;
				break; //enough time wasted
			}
		}
		if ( failed ) {
			Assert.fail( "Message not received after waiting for long!" );
		}
	}

	private int countByQuery(String luceneQueryString) throws ParseException {
		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		try {
			ftSess.getTransaction().begin();
			try {
				Query luceneQuery = parser.parse( luceneQueryString );
				org.hibernate.Query query = ftSess.createFullTextQuery( luceneQuery );
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
		final String indexManagerName = "org.hibernate.search.test.jgroups.master.TShirt";
		IndexManager indexManager = getExtendedSearchIntegrator().getIndexManagerHolder().getIndexManager( indexManagerName );
		//send message to all listeners
		byte[] data = indexManager.getSerializer().toSerializedModel( queue );
		data = MessageSerializationHelper.prependString( indexManagerName, data );
		Message message = new Message( null, null, data );
		channel.send( message );
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
	 * Create a test object and delete if from index.
	 *
	 * @return a <code>TShirt</code> test object.
	 */
	private TShirt createObjectWithSQL() {
		Session s = openSession();
		s.getTransaction().begin();
		TShirt ts = new TShirt();
		ts.setLogo( "JBoss balls" );
		ts.setSize( "large" );
		ts.setLength( 23.2d );
		s.persist( ts );
		s.getTransaction().commit();
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		fullTextSession.beginTransaction();
		fullTextSession.purge( TShirt.class, 1 );
		fullTextSession.getTransaction().commit();
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
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// JGroups configuration for master node
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.setProperty( DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( DispatchMessageSender.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}
}
