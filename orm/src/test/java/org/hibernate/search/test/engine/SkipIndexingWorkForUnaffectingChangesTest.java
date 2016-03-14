/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.Map;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.backend.LeakingLocalBackend;
import org.junit.Assert;
import org.junit.Test;

/**
 * See HSEARCH-361 and HSEARCH-5 : avoid reindexing objects for which
 * changes where made in hibernate but not affecting the index state.
 *
 * @author Sanne Grinovero
 */
public class SkipIndexingWorkForUnaffectingChangesTest extends SearchTestBase {

	@Test
	public void testUnindexedFieldsDontTriggerEngine() {
		// first, normal storage of new indexed graph:
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		BusLine line1 = new BusLine();
		line1.setBusLineCode( 1 );
		line1.setBusLineName( "Line Uno" );
		LazyCollectionsUpdatingTest.addBusStop( line1, "Gateshead" );
		LazyCollectionsUpdatingTest.addBusStop( line1, "The Sage" );
		getSession().persist( line1 );
		tx.commit();

		Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );
		LeakingLocalBackend.reset();
		fullTextSession.clear();

		// now change the BusLine in some way which does not affect the index:
		tx = fullTextSession.beginTransaction();
		line1 = fullTextSession.load( BusLine.class, line1.getId() );
		line1.setBusLineCode( 2 );
		line1.setOperating( true ); // boolean set to same value: might receive a different instance of Boolean
		BusStop busStop = line1.getStops().iterator().next();
		busStop.setServiceComments( "please clean the garbage after the football match" );
		tx.commit();
		if ( isDirtyCheckEnabled() ) {
			Assert.assertEquals( 0, LeakingLocalBackend.getLastProcessedQueue().size() );
		}
		else {
			Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );
		}

		// now we make an indexing affecting change in the embedded object only,
		// parent should still be updated
		LeakingLocalBackend.reset();
		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();
		busStop = fullTextSession.load( BusStop.class, busStop.getId() );
		busStop.setRoadName( "Mill Road" );
		tx.commit();
		Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );

		LeakingLocalBackend.reset();
		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();
		busStop = fullTextSession.load( BusStop.class, busStop.getId() );
		//verify mutable property dirty-ness:
		busStop.getStartingDate().setTime( 0L );
		tx.commit();
		Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );

		LeakingLocalBackend.reset();
		fullTextSession.close();
	}

	// Test setup options - Entities
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { BusLine.class, BusStop.class };
	}

	// Test setup options - SessionFactory Properties
	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
		cfg.put( "hibernate.search.default.worker.backend", LeakingLocalBackend.class.getName() );
	}

	protected boolean isDirtyCheckEnabled() {
		return true;
	}
}
