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
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.LoadEventListener;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.embedded.depth.LeakingLuceneBackend;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

/**
 * Related to HSEARCH-782: make sure we don't unnecessarily index entities or load unrelated entities
 * 
 * @author Adam Harris
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CollectionUpdateEventTest2 {

	private LoadCountingListener loadCountListener;

	@Test
	public void testScenario() {
		
		FullTextSessionBuilder fullTextSessionBuilder = createSearchFactory();
		try {
			assertOperationsPerformed( 0 );
			assertLocationsLoaded( 0 );
			initializeData( fullTextSessionBuilder );
			assertOperationsPerformed( 5 );
			assertLocationsLoaded( 0 );
			FullTextSession fullTextSession = fullTextSessionBuilder.openFullTextSession();
			assertFoundLocations( fullTextSession, "floor", 5 );
			assertFoundLocations( fullTextSession, "airport", 0 );
			fullTextSession.clear();
			try {
				addLocationToGroupCollection( fullTextSession );
				fullTextSession.clear();
				assertLocationsLoaded( 0 );
				assertOperationsPerformed( 1 );
				assertFoundLocations( fullTextSession, "floor", 6 );
				updateLocationGroupName( fullTextSession );
				fullTextSession.clear();
				assertFoundLocations( fullTextSession, "floor", 0 );
				assertFoundLocations( fullTextSession, "airport", 6 );
				assertLocationsLoaded( 6 );
				assertOperationsPerformed( 12 ); // it's an update: 2*6 operations
			}
			finally {
				fullTextSession.close();
			}
		}
		finally {
			fullTextSessionBuilder.close();
		}
	}

	private void assertLocationsLoaded(int expectedLoads) {
		Assert.assertEquals( expectedLoads, loadCountListener.locationLoadEvents.get() );
	}

	private void assertOperationsPerformed(int expectedOperationCount) {
		List<LuceneWork> lastProcessedQueue = LeakingLuceneBackend.getLastProcessedQueue();
		Assert.assertEquals( expectedOperationCount, lastProcessedQueue.size() );
		LeakingLuceneBackend.reset();
	}

	private FullTextSessionBuilder createSearchFactory() {
		loadCountListener = new LoadCountingListener();
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.setProperty( "hibernate.search.worker.backend",
						org.hibernate.search.test.embedded.depth.LeakingLuceneBackend.class.getName() )
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

	private void addLocationToGroupCollection(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LocationGroup group = (LocationGroup) fullTextSession.get( LocationGroup.class, 1L );

		Location location = new Location( "New Room" );
		fullTextSession.persist( location );

		group.getLocations().add( location );
		location.setLocationGroup( group );

		transaction.commit();
	}

	private void updateLocationGroupName(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();

		LocationGroup group = (LocationGroup) fullTextSession.get( LocationGroup.class, 1L );
		LocationGroup locationGroup = (LocationGroup) fullTextSession.merge( group );
		locationGroup.setName( "Airport" );

		transaction.commit();
	}

	private void assertFoundLocations(FullTextSession fullTextSession, String locationGroupName, int expectedFoundLocations) {
		final Transaction transaction = fullTextSession.beginTransaction();
		TermQuery luceneQuery = new TermQuery( new Term( "locationGroup.name", locationGroupName ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Location.class );
		int resultSize = fullTextQuery.getResultSize();
		transaction.commit();
		
		Assert.assertEquals( expectedFoundLocations, resultSize );
	}

	public static class LoadCountingListener implements LoadEventListener {
		final AtomicInteger locationLoadEvents = new AtomicInteger();
		public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
			if ( Location.class.getName().equals( event.getEntityClassName() ) ) {
				locationLoadEvents.incrementAndGet();
			}
		}
	}

}
