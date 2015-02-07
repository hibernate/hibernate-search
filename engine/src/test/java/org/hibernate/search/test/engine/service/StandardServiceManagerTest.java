/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.service;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the standard implementation of the {@code ServiceManager} interface.
 *
 * @author Hardy Ferentschik
 */
public class StandardServiceManagerTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	private SearchConfigurationForTest searchConfiguration;
	private ServiceManager serviceManagerUnderTest;

	@Before
	public void setUp() {
		searchConfiguration = new SearchConfigurationForTest();
		serviceManagerUnderTest = new StandardServiceManager(
				searchConfiguration,
				new DummyBuildContext()
		);
	}

	@Test
	public void testUnavailableServiceThrowsException() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000196" ) );
		serviceManagerUnderTest.requestService( NonExistentService.class );
	}

	@Test
	public void testNullParameterForRequestServiceThrowsException() {
		thrown.expect( IllegalArgumentException.class );
		serviceManagerUnderTest.requestService( null );
	}

	@Test
	public void testNullParameterForReleaseServiceThrowsException() {
		thrown.expect( IllegalArgumentException.class );
		serviceManagerUnderTest.releaseService( null );
	}

	@Test
	public void testMultipleServiceImplementationsThrowsException() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000195" ) );
		serviceManagerUnderTest.requestService( ServiceWithMultipleImplementations.class );
	}

	@Test
	public void testRetrieveService() {
		SimpleService simpleService = serviceManagerUnderTest.requestService( SimpleService.class );
		assertNotNull( "The service should be created", simpleService );
		assertTrue( simpleService instanceof SimpleServiceImpl );
	}

	@Test
	public void testStartServiceIsPerformed() {
		StartableService service = serviceManagerUnderTest.requestService( StartableService.class );
		assertNotNull( "The service should be created", service );
		assertTrue( service instanceof StartableServiceImpl );
		assertTrue( "Service should have been started", ( (StartableServiceImpl) service ).isStarted() );
	}

	@Test
	public void testStopServiceIsPerformed() {
		StoppableService service = serviceManagerUnderTest.requestService( StoppableService.class );

		assertNotNull( "The service should be created", service );
		assertTrue( service instanceof StoppableServiceImpl );

		serviceManagerUnderTest.releaseService( StoppableService.class );
		assertTrue( "Service should have been stopped", ( (StoppableServiceImpl) service ).isStopped() );
	}

	@Test
	public void testServiceInstanceIsCached() {
		SimpleService simpleService1 = serviceManagerUnderTest.requestService( SimpleService.class );
		assertNotNull( "The service should be created", simpleService1 );

		SimpleService simpleService2 = serviceManagerUnderTest.requestService( SimpleService.class );
		assertNotNull( "The service should be created", simpleService2 );
		assertTrue( "The same service instance should have been returned", simpleService1 == simpleService2 );
	}

	@Test
	public void providedServicesHavePrecedence() {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest();
		configuration.getProvidedServices().put( SimpleService.class, new ProgrammaticallyConfiguredSimpleService() );

		serviceManagerUnderTest = new StandardServiceManager(
				configuration,
				new DummyBuildContext()
		);

		SimpleService simpleService = serviceManagerUnderTest.requestService( SimpleService.class );
		assertNotNull( "The service should be created", simpleService );
		assertTrue(
				"Wrong service type: " + simpleService.getClass(),
				simpleService instanceof ProgrammaticallyConfiguredSimpleService
		);
	}

	@Test
	public void testCircularServiceDependencyThrowsException() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000198" ) );

		serviceManagerUnderTest.requestService( FooService.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1547")
	public void testRequestingServiceAfterReleaseAllThrowsException() {
		SimpleService simpleService1 = serviceManagerUnderTest.requestService( SimpleService.class );
		assertNotNull( "The service should be created", simpleService1 );

		serviceManagerUnderTest.releaseAllServices();

		thrown.expect( IllegalStateException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000209" ) );
		serviceManagerUnderTest.requestService( SimpleService.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1547")
	public void testProvidedServicesCannotImplementStartable() {
		searchConfiguration = new SearchConfigurationForTest();

		thrown.expect( SearchException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000210" ) );
		searchConfiguration.addProvidedService(
				StartableProvidedService.class,
				new StartableProvidedServiceImpl()
		);
		serviceManagerUnderTest = new StandardServiceManager(
				searchConfiguration,
				new DummyBuildContext()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1547")
	public void testProvidedServicesCannotImplementStoppable() {
		searchConfiguration = new SearchConfigurationForTest();

		thrown.expect( SearchException.class );
		thrown.expectMessage( JUnitMatchers.containsString( "HSEARCH000210" ) );
		searchConfiguration.addProvidedService(
				StoppableProvidedService.class,
				new StoppableProvidedServiceImpl()
		);
		serviceManagerUnderTest = new StandardServiceManager(
				searchConfiguration,
				new DummyBuildContext()
		);
	}

	// actual impl is not relevant for testing the service manager
	// build context is passed through to services which might need it for initialization
	private class DummyBuildContext implements BuildContext {

		@Override
		public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
			return null;
		}

		@Override
		public String getIndexingStrategy() {
			return null;
		}

		@Override
		public IndexingMode getIndexingMode() {
			return null;
		}

		@Override
		public ServiceManager getServiceManager() {
			// return the service manager under test for circularity test
			return serviceManagerUnderTest;
		}

		@Override
		public IndexManagerHolder getAllIndexesManager() {
			return null;
		}

		@Override
		public ErrorHandler getErrorHandler() {
			return null;
		}
	}

}
