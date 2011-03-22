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
package org.hibernate.search.test.jgroups.common;

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessorFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.jgroups.master.TShirt;

/**
 * In case of running test outside Hibernate Search Maven configuration set following VM configuration:
 * <br>
 * <code>
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 * </code>
 *
 * @author Lukasz Moren
 */
public class JGroupsCommonTest extends MultipleSessionsSearchTestCase {

	private static final String DEFAULT_JGROUPS_CONFIGURATION_FILE = "testing-flush-udp.xml";
	public static final long NETWORK_TIMEOUT = 50;

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

		Thread.sleep( NETWORK_TIMEOUT );

		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		ftSess.getTransaction().begin();
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "id", SearchTestCase.stopAnalyzer );
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
		Thread.sleep( NETWORK_TIMEOUT );

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
		Thread.sleep( NETWORK_TIMEOUT );

		luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
		query = ftSess.createFullTextQuery( luceneQuery );
		result = query.list();
		assertEquals( 0, result.size() );
		ftSess.getTransaction().commit();
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

	protected Class<?>[] getCommonAnnotatedClasses() {
		return new Class[] {
				TShirt.class
		};
	}

}
