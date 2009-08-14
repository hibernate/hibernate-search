// $Id$
package org.hibernate.search.test.jgroups.common;

import java.util.List;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessorFactory;
import org.hibernate.search.test.jgroups.master.TShirt;

/**
 * In case of running test outside Hibernate Search Maven configuration set following VM configuration:
 * <br><br>
 * <code>
 * 	-Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 * </code>
 * @author Lukasz Moren
 */

public class JGroupsCommonTest extends MultipleSessionsSearchTestCase {

	public static final String CHANNEL_NAME = "jgroups_test_channel";
	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "flush-udp.xml";

	public void testJGroupsBackend() throws Exception {

		//get slave session
		Session s = getSlaveSession();
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

		Thread.sleep( 3000 );

		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		ftSess.getTransaction().begin();
		QueryParser parser = new QueryParser( "id", new StopAnalyzer() );
		Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
		org.hibernate.Query query = ftSess.createFullTextQuery( luceneQuery );
		List result = query.list();

		assertEquals( 2, result.size() );

		s = getSlaveSession();
		tx = s.beginTransaction();
		ts = ( TShirt ) s.get( TShirt.class, ts.getId() );
		ts.setLogo( "Peter pan" );
		tx.commit();

		//need to sleep for the message consumption
		Thread.sleep( 3000 );

		parser = new QueryParser( "id", new StopAnalyzer() );
		luceneQuery = parser.parse( "logo:Peter pan" );
		query = ftSess.createFullTextQuery( luceneQuery );
		result = query.list();
		assertEquals( 1, result.size() );

		s = getSlaveSession();
		tx = s.beginTransaction();
		s.delete( s.get( TShirt.class, ts.getId() ) );
		s.delete( s.get( TShirt.class, ts2.getId() ) );
		tx.commit();

		//Need to sleep for the message consumption
		Thread.sleep( 3000 );

		parser = new QueryParser( "id", new StopAnalyzer() );
		luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
		query = ftSess.createFullTextQuery( luceneQuery );
		result = query.list();
		assertEquals( 0, result.size() );

		ftSess.close();
		s.close();

	}

	@Override
	protected void configure(Configuration cfg) {
		//master jgroups configuration
		super.configure( cfg );
		cfg.setProperty( Environment.WORKER_BACKEND, "jgroupsMaster" );
		cfg.setProperty( JGroupsBackendQueueProcessorFactory.CONFIGURATION_FILE, DEFAULT_JGROUPS_CONFIGURATION_FILE );
	}

	@Override
	protected void commonConfigure(Configuration cfg) {
		//slave jgroups configuration
		super.commonConfigure( cfg );
		cfg.setProperty( Environment.WORKER_BACKEND, "jgroupsSlave" );
		cfg.setProperty( JGroupsBackendQueueProcessorFactory.CONFIGURATION_FILE, DEFAULT_JGROUPS_CONFIGURATION_FILE );
	}

	public static Session getSession() throws HibernateException {
		return sessions.openSession();
	}

	@Override
	protected Class<?>[] getMappings() {
		return new Class[] {
				TShirt.class
		};
	}

	protected Class<?>[] getCommonMappings() {
		return new Class[] {
				TShirt.class
		};
	}


}
