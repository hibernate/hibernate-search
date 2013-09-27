/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.engine.optimizations;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.util.LeakingLuceneBackend;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.Test;

/**
 * Related to HSEARCH-782: make sure we don't unnecessarily index entities or load unrelated entities
 *
 * @author Adam Harris
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CollectionUpdateEventsSecondTest {

	private LoadCountingListener loadCountListener;

	@Test
	public void testScenario() {

		FullTextSessionBuilder fullTextSessionBuilder = createSearchFactory();
		try {
			//check no operations are done:
			assertOperationsPerformed( 0 );
			assertLocationsLoaded( 0 );
			//create initial data
			initializeData( fullTextSessionBuilder );
			//this should have triggered 5 indexing operations, no entity loadings:
			assertOperationsPerformed( 5 );
			assertLocationsLoaded( 0 );
			FullTextSession fullTextSession = fullTextSessionBuilder.openFullTextSession();
			//now check index state:
			assertFoundLocations( fullTextSession, "floor", 5 );
			assertFoundLocations( fullTextSession, "airport", 0 );
			fullTextSession.clear();
			try {
				//we add a new Location to the group:
				addLocationToGroupCollection( fullTextSession );
				//NOTHING else should be loaded, there was no need to reindex unrelated Locations!
				assertLocationsLoaded( 0 );
				//of course the new Location should have been indexed:
				assertOperationsPerformed( 1 );
				fullTextSession.clear();
				//so now we have 6 Locations in the index, in LocationGroup "floor":
				assertFoundLocations( fullTextSession, "floor", 6 );
				assertFoundLocations( fullTextSession, "airport", 0 );
				//changing the locationGroup name to Airport:
				updateLocationGroupName( fullTextSession );
				fullTextSession.clear();
				//check index functionality:
				assertFoundLocations( fullTextSession, "floor", 0 );
				assertFoundLocations( fullTextSession, "airport", 6 );
				//six locations have been loaded for re-indexing:
				assertLocationsLoaded( 6 );
				//and six update operations have been sent to the backend:
				assertOperationsPerformed( 6 );
			}
			finally {
				fullTextSession.close();
			}
		}
		finally {
			fullTextSessionBuilder.close();
		}
	}

	/**
	 * Checks the Hibernate Core event listener for how many loads we performed on the
	 * Locations entity. Unexpected value leads to test failure.
	 * (Counter reset after check)
	 */
	private void assertLocationsLoaded(int expectedLoads) {
		Assert.assertEquals( expectedLoads, loadCountListener.locationLoadEvents.getAndSet( 0 ) );
	}

	/**
	 * Asserts we sent a specific amount of LuceneWork operations to the indexing backend.
	 * Counter is reset after invocation.
	 */
	private void assertOperationsPerformed(int expectedOperationCount) {
		List<LuceneWork> lastProcessedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		Assert.assertEquals( expectedOperationCount, lastProcessedQueue.size() );
		LeakingLuceneBackend.reset();
	}

	private FullTextSessionBuilder createSearchFactory() {
		loadCountListener = new LoadCountingListener();
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.setProperty( "hibernate.search.default.worker.backend",
						LeakingLuceneBackend.class.getName() )
				.addAnnotatedClass( LocationGroup.class )
				.addAnnotatedClass( Location.class )
				.addLoadEventListener( loadCountListener );
		return builder.build();
	}

	/**
	 * Initialize the test data.
	 *
	 * @param fulltextSessionBuilder
	 */
	private void initializeData(FullTextSessionBuilder fulltextSessionBuilder) {
		FullTextSession fullTextSession = fulltextSessionBuilder.openFullTextSession();
		try {
			final Transaction transaction = fullTextSession.beginTransaction();

			LocationGroup group = new LocationGroup( "Floor 1" );
			fullTextSession.persist( group );

			for ( int i = 0; i < 5; i++ ) {
				Location location = new Location( "Room 10" + i );
				fullTextSession.persist( location );

				group.getLocations().add( location );
				location.setLocationGroup( group );
				fullTextSession.merge( group );
			}
			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	/**
	 * Adds a single Location to the LocationGroup#1
	 */
	private void addLocationToGroupCollection(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LocationGroup group = (LocationGroup) fullTextSession.get( LocationGroup.class, 1L );

		Location location = new Location( "New Room" );
		fullTextSession.persist( location );

		group.getLocations().add( location );
		location.setLocationGroup( group );

		transaction.commit();
	}

	/**
	 * Changes the parent LocationGroup's name to "Airport"
	 */
	private void updateLocationGroupName(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();

		LocationGroup group = (LocationGroup) fullTextSession.get( LocationGroup.class, 1L );
		LocationGroup locationGroup = (LocationGroup) fullTextSession.merge( group );
		locationGroup.setName( "Airport" );

		transaction.commit();
	}

	/**
	 * Creates a full-text query on Locations entities and checks the term is found exactly
	 * the expected number of times (or fails the test)
	 */
	private void assertFoundLocations(FullTextSession fullTextSession, String locationGroupName, int expectedFoundLocations) {
		final Transaction transaction = fullTextSession.beginTransaction();
		TermQuery luceneQuery = new TermQuery( new Term( "locationGroup.name", locationGroupName ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Location.class );
		int resultSize = fullTextQuery.getResultSize();
		transaction.commit();

		Assert.assertEquals( expectedFoundLocations, resultSize );
	}

	/**
	 * We count the load events of Location entities so that we can
	 * test the event happens only when needed.
	 */
	public static class LoadCountingListener implements LoadEventListener {
		final AtomicInteger locationLoadEvents = new AtomicInteger();
		@Override
		public void onLoad(LoadEvent event, LoadType loadType) {
			if ( Location.class.getName().equals( event.getEntityClassName() ) ) {
				locationLoadEvents.incrementAndGet();
			}
		}
	}

}
