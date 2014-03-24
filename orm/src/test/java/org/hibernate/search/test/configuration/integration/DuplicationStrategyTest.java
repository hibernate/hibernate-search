/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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


