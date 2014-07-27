/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.assertFieldSelectorDisabled;
import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.assertFieldSelectorEnabled;
import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.resetFieldSelector;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * TestCase for HSEARCH-178 (Search hitting HHH-2763)
 * Verifies that it's possible to index lazy loaded collections from
 * indexed entities even when no transactions are used.
 *
 * Additionally, it uses projection and verifies an optimal FieldSelector
 * is being applied (HSEARCH-690).
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
			resetFieldSelector();
			List list = fullTextSession.createCriteria( BusStop.class ).list();
			assertFieldSelectorDisabled();
			assertNotNull( list );
			assertEquals( 4, list.size() );
			BusStop busStop = (BusStop) list.get( 1 );
			busStop.setRoadName( "new road" );
			tx.commit();
		}
		catch (org.hibernate.annotations.common.AssertionFailure ass) {
			fail( ass.getMessage() );
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
			List list = fullTextSession.createCriteria( BusStop.class ).list();
			assertNotNull( list );
			assertEquals( 4, list.size() );
			BusStop busStop = (BusStop) list.get( 1 );
			busStop.setRoadName( "new road" );
			fullTextSession.flush();
		}
		catch (org.hibernate.annotations.common.AssertionFailure ass) {
			fail( ass.getMessage() );
		}
		finally {
			fullTextSession.close();
		}
		assertFindsByRoadName( "new" );
	}

	public void assertFindsByRoadName(String analyzedRoadname) {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		resetFieldSelector();
		Transaction tx = fullTextSession.beginTransaction();
		TermQuery ftQuery = new TermQuery( new Term( "stops.roadName", analyzedRoadname ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( ftQuery, BusLine.class );
		query.setProjection( "busLineName" );
		assertEquals( 1, query.list().size() );
		List results = query.list();
		try {
			assertFieldSelectorEnabled( "busLineName" );
		}
		catch (IOException e) {
			fail( "unexpected exception " + e );
		}
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
			addBusStop( bus, "via M.Buonarroti" );
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	protected void configure(org.hibernate.cfg.Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( "hibernate.search.default." + Environment.READER_STRATEGY, FieldSelectorLeakingReaderProvider.class.getName() );
		configuration.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}

}
