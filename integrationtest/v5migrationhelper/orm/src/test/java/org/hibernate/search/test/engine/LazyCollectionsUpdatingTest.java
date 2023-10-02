/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * TestCase for HSEARCH-178 (Search hitting HHH-2763)
 * Verifies that it's possible to index lazy loaded collections from
 * indexed entities even when no transactions are used.
 *
 * @author Sanne Grinovero
 */
class LazyCollectionsUpdatingTest extends SearchTestBase {

	@Test
	void testUpdatingInTransaction() {
		assertFindsByRoadName( "buonarroti" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction tx = fullTextSession.beginTransaction();
			List list = listAll( fullTextSession, BusStop.class );
			assertThat( list ).isNotNull()
					.hasSize( 4 );
			BusStop busStop = (BusStop) list.get( 1 );
			busStop.setRoadName( "new road" );
			tx.commit();
		}
		finally {
			fullTextSession.close();
		}
		assertFindsByRoadName( "new" );
	}

	@Test
	void testUpdatingOutOfTransaction() {
		assertFindsByRoadName( "buonarroti" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			List list = listAll( fullTextSession, BusStop.class );
			assertThat( list ).isNotNull()
					.hasSize( 4 );
			BusStop busStop = (BusStop) list.get( 1 );
			busStop.setRoadName( "new road" );
			fullTextSession.flush();
		}
		finally {
			fullTextSession.close();
		}
		assertFindsByRoadName( "new" );
	}

	public void assertFindsByRoadName(String analyzedRoadname) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		TermQuery ftQuery = new TermQuery( new Term( "stops.roadName", analyzedRoadname ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( ftQuery, BusLine.class );
		query.setProjection( "busLineName" );
		assertThat( query.list() ).hasSize( 1 );
		List results = query.list();
		String resultName = (String) ( (Object[]) results.get( 0 ) )[0];
		assertThat( resultName ).isEqualTo( "Linea 64" );
		tx.commit();
		fullTextSession.close();
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		openSession();
		Transaction tx = null;
		try {
			tx = getSession().beginTransaction();
			BusLine bus = new BusLine();
			bus.setBusLineName( "Linea 64" );
			addBusStop( bus, "Stazione Termini" );
			addBusStop( bus, "via Gregorio VII" );
			addBusStop( bus, "via Alessandro III" );
			addBusStop( bus, "via M. Buonarroti" );
			getSession().persist( bus );
			tx.commit();
		}
		catch (Throwable t) {
			if ( tx != null ) {
				tx.rollback();
			}
		}
		finally {
			getSession().close();
		}
	}

	static void addBusStop(BusLine bus, String roadName) {
		BusStop stop = new BusStop();
		stop.setRoadName( roadName );
		bus.getStops().add( stop );
		stop.getBusses().add( bus );
	}

	// Test setup options - Entities
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( "hibernate.allow_update_outside_transaction", "true" );
	}

}
