/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import java.util.LinkedHashSet;
import java.util.Properties;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.Mock;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.hibernate.search.hcore.impl.SearchFactoryReference;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

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
	private EventListenerRegistry mockEventListenerRegistry;

	@Mock
	private ClassLoaderService mockClassLoaderService;

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
		expect( mockSessionFactoryServiceRegistry.getService( SearchFactoryReference.class ) ).andReturn(
				new SearchFactoryReference()
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

		expect( mockSessionFactoryServiceRegistry.getService( ClassLoaderService.class ) ).andReturn(
				mockClassLoaderService
		);

		expect( mockSessionFactoryImplementor.getServiceRegistry() ).andReturn(
				mockSessionFactoryServiceRegistry
		);

		// returning object.class is fair enough for testing purposes
		expect( mockClassLoaderService.classForName( "javax.persistence.Id" ) ).andReturn( Object.class );

		expect( mockClassLoaderService.loadJavaServices( BridgeProvider.class ) ).andReturn(
				new LinkedHashSet<BridgeProvider>(
						0
				)
		);

		EasyMockUnitils.replay();

		integratorUnderTest.integrate( cfg, mockSessionFactoryImplementor, mockSessionFactoryServiceRegistry );

		capturedSessionFactoryObserver.getValue().sessionFactoryCreated( mockSessionFactoryImplementor );
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
