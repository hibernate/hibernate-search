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
package org.hibernate.search.test.configuration;

import java.util.Properties;

import org.hibernate.event.EventListeners;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.search.Environment;
import org.hibernate.search.event.EventListenerRegister;
import org.hibernate.search.event.FullTextIndexEventListener;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sanne Grinovero
 */
public class EventListenerRegisterTest {

	@Test
	public void testRegisterOnEmptyListeners_CfgDisabled() {
		EventListeners evListeners = new EventListeners();
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( false ) );
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( false ) );
		assertPresence( false, evListeners );
	}
	
	@Test
	public void testRegisterOnEmptyListeners_CfgEnabled() {
		EventListeners evListeners = new EventListeners();
		//tests registering multiple times is idempotent:
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( true ) );
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( true ) );
		assertPresence( true, evListeners );
	}

	@Test
	public void testRegisterOnEmptyListeners_CfgAuto() {
		EventListeners evListeners = new EventListeners();
		EventListenerRegister.enableHibernateSearch( evListeners, new Properties() );
		EventListenerRegister.enableHibernateSearch( evListeners, new Properties() );
		assertPresence( true, evListeners );
	}

	@Test
	public void testOnAlreadyRegistered() {
		helperOnAlreadyRegistered( new FullTextIndexEventListener(FullTextIndexEventListener.Installation.SINGLE_INSTANCE) );
	}

	@Test
	public void testOnPopulatedEventListeners() {
		EventListeners evListeners = makeSomeEventListeners();
		EventListenerRegister.enableHibernateSearch( evListeners, new Properties() );
		EventListenerRegister.enableHibernateSearch( evListeners, new Properties() );
		assertPresence( true, evListeners );
	}

	private void helperOnAlreadyRegistered(FullTextIndexEventListener listenerFullText) {

		AnotherListener listenerA = new AnotherListener();
		AnotherListener listenerB = new AnotherListener();

		EventListeners evListeners = new EventListeners();
		evListeners.setPostInsertEventListeners(
				new PostInsertEventListener[] { listenerA, listenerB, listenerFullText }
		);
		evListeners.setPostUpdateEventListeners(
				new PostUpdateEventListener[] { listenerA, listenerB, listenerFullText }
		);
		evListeners.setPostDeleteEventListeners(
				new PostDeleteEventListener[] { listenerA, listenerB, listenerFullText }
		);
		evListeners.setPostCollectionRecreateEventListeners(
				new PostCollectionRecreateEventListener[] { listenerA, listenerB, listenerFullText }
		);
		evListeners.setPostCollectionRemoveEventListeners(
				new PostCollectionRemoveEventListener[] { listenerA, listenerB, listenerFullText }
		);
		evListeners.setPostCollectionUpdateEventListeners(
				new PostCollectionUpdateEventListener[] { listenerA, listenerB, listenerFullText }
		);

		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( false ) );
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( false ) );
		EventListenerRegister.enableHibernateSearch( evListeners, makeConfiguration( false ) );
		assertPresence( true, evListeners );
	}

	private EventListeners makeSomeEventListeners() {

		AnotherListener listenerA = new AnotherListener();
		AnotherListener listenerB = new AnotherListener();
		AnotherListener listenerC = new AnotherListener();

		EventListeners evListeners = new EventListeners();
		evListeners.setPostInsertEventListeners(
				new PostInsertEventListener[] { listenerA, listenerB, listenerC }
		);
		evListeners.setPostUpdateEventListeners(
				new PostUpdateEventListener[] { listenerA, listenerB, listenerC }
		);
		evListeners.setPostDeleteEventListeners(
				new PostDeleteEventListener[] { listenerA, listenerB, listenerC }
		);
		evListeners.setPostCollectionRecreateEventListeners(
				new PostCollectionRecreateEventListener[] { listenerA, listenerB, listenerC }
		);
		evListeners.setPostCollectionRemoveEventListeners(
				new PostCollectionRemoveEventListener[] { listenerA, listenerB, listenerC }
		);
		evListeners.setPostCollectionUpdateEventListeners(
				new PostCollectionUpdateEventListener[] { listenerA, listenerB, listenerC }
		);

		return evListeners;
	}

	private void assertPresence(boolean expected, EventListeners evListeners) {
		assertEquals( expected, isPresent( evListeners.getPostInsertEventListeners() ) );
		assertEquals( expected, isPresent( evListeners.getPostUpdateEventListeners() ) );
		assertEquals( expected, isPresent( evListeners.getPostDeleteEventListeners() ) );
		assertEquals( expected, isPresent( evListeners.getPostCollectionRecreateEventListeners() ) );
		assertEquals( expected, isPresent( evListeners.getPostCollectionRemoveEventListeners() ) );
		assertEquals( expected, isPresent( evListeners.getPostCollectionUpdateEventListeners() ) );
	}

	private static Properties makeConfiguration(boolean enableSearch) {
		Properties p = new Properties();
		p.setProperty( Environment.AUTOREGISTER_LISTENERS, String.valueOf( enableSearch ) );
		return p;
	}

	private static boolean isPresent(Object[] listeners) {
		if ( listeners == null ) {
			return false;
		}
		boolean found = false; // to verify class present at most once.
		for ( Object eventListener : listeners ) {
			if ( FullTextIndexEventListener.class == eventListener.getClass() ) {
				assertFalse( found );
				found = true;
			}
		}
		return found;
	}

	private static class AnotherListener implements PostDeleteEventListener,
			PostInsertEventListener, PostUpdateEventListener,
			PostCollectionRecreateEventListener, PostCollectionRemoveEventListener,
			PostCollectionUpdateEventListener {

		//empty methods: just needing any implementation of these listeners.

		public void onPostDelete(PostDeleteEvent event) {
		}

		public void onPostInsert(PostInsertEvent event) {
		}

		public void onPostUpdate(PostUpdateEvent event) {
		}

		public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		}

		public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		}

		public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		}
	}
}
