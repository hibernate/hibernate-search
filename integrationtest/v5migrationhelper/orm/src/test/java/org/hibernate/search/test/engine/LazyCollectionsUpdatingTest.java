/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * TestCase for HSEARCH-178 (Search hitting HHH-2763)
 * Verifies that it's possible to index lazy loaded collections from
 * indexed entities even when no transactions are used.
 *
 * @author Sanne Grinovero
 */
public class LazyCollectionsUpdatingTest extends SearchTestBase {

	@Test
	public void testUpdatingInTransaction() {
		assertFindsByRoadName( "buonarroti" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			Transaction tx = fullTextSession.beginTransaction();
			List list = listAll( fullTextSession, BusStop.class );
			assertNotNull( list );
			assertEquals( 4, list.size() );
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
	public void testUpdatingOutOfTransaction() {
		assertFindsByRoadName( "buonarroti" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		try {
			List list = listAll( fullTextSession, BusStop.class );
			assertNotNull( list );
			assertEquals( 4, list.size() );
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
		assertEquals( 1, query.list().size() );
		List results = query.list();
		String resultName = (String) ( (Object[]) results.get( 0 ) )[0];
		assertEquals( "Linea 64", resultName );
		tx.commit();
		fullTextSession.close();
	}

	@Override
	@Before
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
