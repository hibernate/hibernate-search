/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class DuplicationStrategyTest {

	@Test
	public void testMultipleRegistrationOfEventListenerKeepsOriginalListener() {
		EventListenerRegistry eventListenerRegistry = new EventListenerRegistryImpl();

		EventListenerGroup<PostInsertEventListener> eventEventListenerGroup = eventListenerRegistry.getEventListenerGroup(
				EventType.POST_INSERT
		);
		assertTrue( "We should start of with no listeners", eventEventListenerGroup.count() == 0 );

		FullTextIndexEventListener firstFullTextIndexEventListener = new FullTextIndexEventListener();

		eventListenerRegistry.setListeners( EventType.POST_INSERT, firstFullTextIndexEventListener );
		eventListenerRegistry.addDuplicationStrategy(
				new HibernateSearchIntegrator.DuplicationStrategyImpl(
						FullTextIndexEventListener.class
				)
		);
		eventListenerRegistry.appendListeners( EventType.POST_INSERT, new FullTextIndexEventListener() );

		assertTrue(
				"We should only be one listener, but we have " + eventEventListenerGroup.count(),
				eventEventListenerGroup.count() == 1
		);
		assertTrue(
				"The instances should match",
				firstFullTextIndexEventListener == eventEventListenerGroup.listeners().iterator().next()
		);
	}

}


