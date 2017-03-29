/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.startsWith;

import java.util.Collections;
import java.util.HashMap;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.search.bridge.spi.IndexManagerTypeSpecificBridgeProvider;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.hibernate.search.hcore.impl.SearchFactoryReference;
import org.hibernate.search.hcore.spi.BeanResolver;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;
import org.hibernate.search.query.engine.impl.LuceneQueryTranslator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.Mock;

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

	@Mock
	private SessionFactoryOptions mockSessionFactoryOptions;

	@Mock
	private JndiService jndiService;

	@Mock
	private Metadata mockMetadata;

	@Test
	public void testEventListenersAreNotRegisteredIfSearchIsExplicitlyDisabledInConfiguration() {
		programConfigurationMock( SEARCH_DISABLED );

		EasyMockUnitils.replay();

		// Search should not care about the metadata or the factory if it's disabled
		new HibernateSearchIntegrator().integrate( null, null, mockSessionFactoryServiceRegistry );
	}

	@Test
	public void testEventListenersAreRegisteredIfSearchIsExplicitlyEnabledInConfiguration() {
		programConfigurationMock( SEARCH_ENABLED );
		assertObserverCalledAndEventListenersRegistered();
	}

	@Test
	public void testEventListenersAreRegisteredIfSearchIsImplicitlyEnabledInConfiguration() {
		programConfigurationMock( SEARCH_IMPLICITLY_ENABLED );
		assertObserverCalledAndEventListenersRegistered();
	}

	@SuppressWarnings("unchecked")
	private void assertObserverCalledAndEventListenersRegistered() {
		Capture<SessionFactoryObserver> capturedSessionFactoryObserver = new Capture<SessionFactoryObserver>();
		mockSessionFactoryImplementor.addObserver(
				EasyMock.and(
						EasyMock.capture( capturedSessionFactoryObserver ),
						isA( SessionFactoryObserver.class )
				)
		);

		expect( mockSessionFactoryImplementor.getSessionFactoryOptions() ).andReturn(
				mockSessionFactoryOptions
		);

		expect( mockSessionFactoryOptions.getMultiTenancyStrategy() ).andReturn(
				MultiTenancyStrategy.NONE
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

		expect( mockSessionFactoryServiceRegistry.getService( ClassLoaderService.class ) )
			.andReturn( mockClassLoaderService )
			.anyTimes();

		expect( mockSessionFactoryServiceRegistry.locateServiceBinding( BeanResolver.class ) )
			.andReturn( null )
			.anyTimes();

		expect( mockSessionFactoryServiceRegistry.locateServiceBinding( EnvironmentSynchronizer.class ) )
			.andReturn( null )
			.anyTimes();

		// returning object.class is fair enough for testing purposes
		expect( mockClassLoaderService.classForName( "javax.persistence.Id" ) )
			.andReturn( Object.class )
			.anyTimes();

		expect( mockClassLoaderService.classForName( "javax.persistence.EmbeddedId" ) )
			.andReturn( Object.class )
			.anyTimes();

		expect( mockClassLoaderService.loadJavaServices( LuceneQueryTranslator.class ) )
				.andReturn( Collections.<LuceneQueryTranslator>emptySet() );

		expect( mockClassLoaderService.loadJavaServices( IndexManagerTypeSpecificBridgeProvider.class ) )
				.andReturn( Collections.<IndexManagerTypeSpecificBridgeProvider>emptySet() );

		expect( mockClassLoaderService.loadJavaServices( BridgeProvider.class ) )
			.andReturn( Collections.<BridgeProvider>emptySet() );

		expect( mockClassLoaderService.classForName( startsWith( "java.time" ) ) )
			.andThrow( new ClassLoadingException( "Called by JavaTimeBridgeProvider; we assume the classes in java.time are not on the ORM class loader" ) )
			.anyTimes();

		expect( mockMetadata.getEntityBindings() )
			.andReturn( Collections.EMPTY_SET )
			.anyTimes();

		expect( mockSessionFactoryImplementor.getServiceRegistry() )
			.andReturn( mockSessionFactoryServiceRegistry )
			.anyTimes();

		EasyMockUnitils.replay();

		HibernateSearchIntegrator testedIntegrator = new HibernateSearchIntegrator();
		testedIntegrator.integrate( mockMetadata, mockSessionFactoryImplementor, mockSessionFactoryServiceRegistry );

		capturedSessionFactoryObserver.getValue().sessionFactoryCreated( mockSessionFactoryImplementor );
	}

	private void programConfigurationMock(Boolean enableSearch) {
		HashMap<String,String> settings = new HashMap<>();
		if ( enableSearch != null ) {
			settings.put( Environment.AUTOREGISTER_LISTENERS, String.valueOf( enableSearch ) );
		}
		ConfigurationService cfg = new ConfigurationServiceImpl( settings );
		expect( mockSessionFactoryServiceRegistry.getService( ConfigurationService.class ) )
			.andReturn( cfg )
			.anyTimes();
		expect( mockSessionFactoryServiceRegistry.getService( JndiService.class ) )
			.andReturn( jndiService )
			.anyTimes();
	}
}
