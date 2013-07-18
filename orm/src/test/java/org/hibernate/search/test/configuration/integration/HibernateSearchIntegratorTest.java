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
package org.hibernate.search.test.configuration.integration;

import java.util.Properties;

import org.easymock.Capture;
import org.easymock.EasyMock;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.search.Environment;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.Mock;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

/**
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class HibernateSearchIntegratorTest extends UnitilsJUnit4 {

	private static final Boolean SEARCH_DISABLED = Boolean.FALSE;
	private static final Boolean SEARCH_ENABLED = Boolean.TRUE;
	private static final Boolean SEARCH_IMPLICITLY_ENABLED = null;

	@Mock
	private SessionFactoryServiceRegistry mockSessionFactoryServiceRegistry;

	@Mock
	private SessionFactoryImplementor mockSessionFactoryImplementor;

	@Mock
	private SessionFactory mockSessionFactory;

	@Mock
	private EventListenerRegistry mockEventListenerRegistry;

	private HibernateSearchIntegrator integratorUnderTest;

	@Before
	public void setUp() {
		integratorUnderTest = new HibernateSearchIntegrator();
	}

	@Test
	public void testEventListenersAreNotRegisteredIfSearchIsExplicitlyDisabledInConfiguration() {
		Configuration cfg = makeConfiguration( SEARCH_DISABLED );

		// no mock setup, integrator should not call the mocks, because Search is disabled
		EasyMockUnitils.replay();

		integratorUnderTest.integrate( cfg, mockSessionFactoryImplementor, mockSessionFactoryServiceRegistry );
	}

	@Test
	public void testEventListenersAreRegisteredIfSearchIsExplicitlyEnabledInConfiguration() {
		Configuration cfg = makeConfiguration( SEARCH_ENABLED );
		assertObserverCalledAndEventListenersRegistered( cfg );
	}

	@Test
	public void testEventListenersAreRegisteredIfSearchIsImplicitlyEnabledInConfiguration() {
		Configuration cfg = makeConfiguration( SEARCH_IMPLICITLY_ENABLED );
		assertObserverCalledAndEventListenersRegistered( cfg );
	}

	@SuppressWarnings("unchecked")
	private void assertObserverCalledAndEventListenersRegistered(Configuration cfg) {
		Capture<SessionFactoryObserver> capturedSessionFactoryObserver = new Capture<SessionFactoryObserver>();
		mockSessionFactoryImplementor.addObserver(
				EasyMock.and(
						EasyMock.capture( capturedSessionFactoryObserver ),
						isA( SessionFactoryObserver.class )
				)
		);

		expect( mockSessionFactoryServiceRegistry.getService( EventListenerRegistry.class ) ).andReturn(
				mockEventListenerRegistry
		);

		mockEventListenerRegistry.addDuplicationStrategy( isA( HibernateSearchIntegrator.DuplicationStrategyImpl.class ) );

		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_INSERT ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_UPDATE ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_DELETE ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_COLLECTION_RECREATE ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_COLLECTION_REMOVE ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.POST_COLLECTION_UPDATE ),
				isA( FullTextIndexEventListener.class )
		);
		mockEventListenerRegistry.appendListeners(
				eq( EventType.FLUSH ),
				isA( FullTextIndexEventListener.class )
		);

		EasyMockUnitils.replay();

		integratorUnderTest.integrate( cfg, mockSessionFactoryImplementor, mockSessionFactoryServiceRegistry );

		capturedSessionFactoryObserver.getValue().sessionFactoryCreated( mockSessionFactory );
	}

	private static Configuration makeConfiguration(Boolean enableSearch) {
		Configuration cfg = new Configuration();
		Properties properties = new Properties();
		cfg.setProperties( properties );
		if ( enableSearch != null ) {
			properties.setProperty( Environment.AUTOREGISTER_LISTENERS, String.valueOf( enableSearch ) );
		}
		return cfg;
	}
}
