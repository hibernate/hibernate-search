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
package org.hibernate.search.test.jgroups.master;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.jgroups.JChannel;
import org.jgroups.Message;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.backend.impl.jgroups.MessageSerializationHelper;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.jgroups.common.JGroupsCommonTest;
import org.hibernate.search.test.jms.master.TShirt;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Tests that the Master node in a JGroups cluster can properly process messages received from channel.
 * <p/>
 * In case of running test outside Hibernate Search Maven configuration set following VM configuration:
 * <br><br>
 * <code>
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 * </code>
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsMasterTest extends SearchTestCase {

	private final QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	private JChannel channel;

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
		final String indexManagerName = "org.hibernate.search.test.jms.master.TShirt";
		IndexManager indexManager = getSearchFactoryImpl().getIndexManagerHolder().getIndexManager( indexManagerName );
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
	 * Create a test object and delete if from index.
	 *
	 * @return a <code>TShirt</code> test object.
	 */
	private TShirt createObjectWithSQL() {
		Session s = openSession();
		s.getTransaction().begin();
		TShirt ts = new TShirt();
		ts.setId( 1 );
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
	public void setUp() throws Exception {
		prepareJGroupsChannel();
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		channel.close();
		super.tearDown();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// JGroups configuration for master node
		cfg.setProperty( "hibernate.search.default." + Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.setProperty( JGroupsChannelProvider.CLUSTER_NAME, CHANNEL_NAME );
		cfg.setProperty( JGroupsChannelProvider.CONFIGURATION_FILE, "testing-flush-loopback.xml" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}
}
