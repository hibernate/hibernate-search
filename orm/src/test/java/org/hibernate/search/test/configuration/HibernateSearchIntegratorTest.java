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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Properties;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.search.Environment;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;


/**
 * @author Sanne Grinovero
 */
public class HibernateSearchIntegratorTest {

	@Test
	public void testRegisterOnEmptyListeners_CfgDisabled() {
		Configuration cfg = makeConfiguration( false );
		HibernateSearchIntegrator integrator = new HibernateSearchIntegrator();
		SimpleSessionFactoryServiceRegistry serviceRegistry = new SimpleSessionFactoryServiceRegistry();
		integrator.integrate( cfg, null, serviceRegistry );
		assertPresence( false, serviceRegistry );
		integrator.disintegrate( null, serviceRegistry );
	}

	@Test
	public void testRegisterOnEmptyListeners_CfgEnabled() {
		Configuration cfg = makeConfiguration( true );
		HibernateSearchIntegrator integrator = new HibernateSearchIntegrator();
		SimpleSessionFactoryServiceRegistry serviceRegistry = new SimpleSessionFactoryServiceRegistry();
		//FIXME no longer tests registering multiple times as idempotent
		integrator.integrate( cfg, null, serviceRegistry );
		assertPresence( true, serviceRegistry );
		integrator.disintegrate( null, serviceRegistry );
	}

	@Test
	public void testRegisterOnEmptyListeners_CfgAuto() {
		Configuration cfg = makeConfiguration( null );
		HibernateSearchIntegrator integrator = new HibernateSearchIntegrator();
		SimpleSessionFactoryServiceRegistry serviceRegistry = new SimpleSessionFactoryServiceRegistry();
		integrator.integrate( cfg, null, serviceRegistry );
		assertPresence( true, serviceRegistry );
		integrator.disintegrate( null, serviceRegistry );
	}

	@Test
	public void testOnAlreadyRegistered() {
		helperOnAlreadyRegistered( new FullTextIndexEventListener( FullTextIndexEventListener.Installation.SINGLE_INSTANCE ) );
	}

	@Test
	public void testOnPopulatedEventListeners() {
		Configuration cfg = makeConfiguration( null );
		HibernateSearchIntegrator integrator = new HibernateSearchIntegrator();
		SimpleSessionFactoryServiceRegistry serviceRegistry = new SimpleSessionFactoryServiceRegistry();
		makeSomeEventListeners( serviceRegistry );
		integrator.integrate( cfg, null, serviceRegistry );
		assertPresence( true, serviceRegistry );
		integrator.disintegrate( null, serviceRegistry );
	}

	private void helperOnAlreadyRegistered(FullTextIndexEventListener listenerFullText) {
		SimpleSessionFactoryServiceRegistry serviceRegistry = new SimpleSessionFactoryServiceRegistry();

		AnotherListener listenerA = new AnotherListenerA();
		AnotherListener listenerB = new AnotherListenerB();
		final EventListenerRegistry service = serviceRegistry.getService( EventListenerRegistry.class );
		service.getEventListenerGroup( EventType.POST_INSERT )
				.appendListeners( listenerA, listenerB, listenerFullText );
		service.getEventListenerGroup( EventType.POST_UPDATE )
				.appendListeners( listenerA, listenerB, listenerFullText );
		service.getEventListenerGroup( EventType.POST_DELETE )
				.appendListeners( listenerA, listenerB, listenerFullText );
		service.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE )
				.appendListeners( listenerA, listenerB, listenerFullText );
		service.getEventListenerGroup( EventType.POST_COLLECTION_REMOVE ).appendListeners(
				listenerA,
				listenerB,
				listenerFullText
		);
		service.getEventListenerGroup( EventType.POST_COLLECTION_UPDATE )
				.appendListeners( listenerA, listenerB, listenerFullText );
		HibernateSearchIntegrator integrator = new HibernateSearchIntegrator();
		integrator.integrate( makeConfiguration( false ), null, serviceRegistry );
		assertPresence( true, serviceRegistry );
	}

	private void makeSomeEventListeners(ServiceRegistry serviceRegistry) {
		AnotherListener listenerA = new AnotherListenerA();
		AnotherListener listenerB = new AnotherListenerB();
		AnotherListener listenerC = new AnotherListenerC();
		final EventListenerRegistry service = serviceRegistry.getService( EventListenerRegistry.class );
		service.getEventListenerGroup( EventType.POST_INSERT ).appendListeners( listenerA, listenerB, listenerC );
		service.getEventListenerGroup( EventType.POST_UPDATE ).appendListeners( listenerA, listenerB, listenerC );
		service.getEventListenerGroup( EventType.POST_DELETE ).appendListeners( listenerA, listenerB, listenerC );
		service.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE ).appendListeners(
				listenerA,
				listenerB,
				listenerC
		);
		service.getEventListenerGroup( EventType.POST_COLLECTION_REMOVE )
				.appendListeners( listenerA, listenerB, listenerC );
		service.getEventListenerGroup( EventType.POST_COLLECTION_UPDATE )
				.appendListeners( listenerA, listenerB, listenerC );
	}

	private void assertPresence(boolean expected, ServiceRegistry serviceRegistry) {
		final EventListenerRegistry service = serviceRegistry.getService( EventListenerRegistry.class );
		assertEquals( expected, isPresent( service.getEventListenerGroup( EventType.POST_INSERT ).listeners() ) );
		assertEquals( expected, isPresent( service.getEventListenerGroup( EventType.POST_UPDATE ).listeners() ) );
		assertEquals( expected, isPresent( service.getEventListenerGroup( EventType.POST_DELETE ).listeners() ) );
		assertEquals(
				expected,
				isPresent( service.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE ).listeners() )
		);
		assertEquals(
				expected,
				isPresent( service.getEventListenerGroup( EventType.POST_COLLECTION_REMOVE ).listeners() )
		);
		assertEquals(
				expected,
				isPresent( service.getEventListenerGroup( EventType.POST_COLLECTION_UPDATE ).listeners() )
		);
	}

	private static Configuration makeConfiguration(Boolean enableSearch) {
		Configuration cfg = new Configuration();
		Properties p = new Properties();
		cfg.setProperties( p );
		if ( enableSearch != null ) {
			p.setProperty( Environment.AUTOREGISTER_LISTENERS, String.valueOf( enableSearch ) );
		}
		return cfg;
	}

	private static boolean isPresent(Iterable<?> listeners) {
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

	private static class AnotherListenerA extends AnotherListener { }

	private static class AnotherListenerB extends AnotherListener { }

	private static class AnotherListenerC extends AnotherListener { }

	private static class SimpleSessionFactoryServiceRegistry implements SessionFactoryServiceRegistry {

		private final EventListenerRegistryImpl eventListenerRegistry = new EventListenerRegistryImpl();

		@Override
		public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole) {
			return null;
		}

		@Override
		public void destroy() {

		}

		@Override
		public ServiceRegistry getParentServiceRegistry() {
			return null;
		}

		@Override
		public <R extends Service> R getService(Class<R> serviceRole) {
			if ( EventListenerRegistry.class.equals( serviceRole ) ) {
				return (R) eventListenerRegistry;
			}
			else {
				return null;
			}
		}
	}
}
