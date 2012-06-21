package org.hibernate.search.test.service;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.test.util.ManualConfiguration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ServiceProviderTest {

	@Rule
	public ExpectedException exceptions = ExpectedException.none();

	@Test
	public void testManagedService() throws Exception {
		MyServiceProvider.resetActive();
		assertFalse( MyServiceProvider.isActive() );
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration.addProperty( "hibernate.search.default.directory_provider", ServiceDirectoryProvider.class.getName() )
				.addClass( Telephone.class );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		assertTrue( MyServiceProvider.isActive() );
		sf.close();
		assertFalse( MyServiceProvider.isActive() );
	}

	@Test
	public void testCircularDependenciesNotAllowed() throws Exception {
		MyServiceProvider.resetActive();
		assertFalse( MyServiceProvider.isActive() );
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration.addProperty( "hibernate.search.default.directory_provider", ServiceDirectoryProvider.class.getName() )
				.addClass( Telephone.class );

		exceptions.expect( SearchException.class );
		MyServiceProvider.setSimulateCircularDependency( true );
		SearchFactoryImplementor sf = null;
		try {
			sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		}
		finally {
			MyServiceProvider.setSimulateCircularDependency( false );
			if ( sf != null ) {
				sf.close();
			}
		}
	}

	@Test
	public void testProvidedService() throws Exception {
		ProvidedServiceProvider.resetActive();
		assertFalse( ProvidedServiceProvider.isActive() );
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration
				.addProperty( "hibernate.search.default.directory_provider", ProvidedServiceDirectoryProvider.class.getName() )
				.addClass( Telephone.class )
				.getProvidedServices().put( ProvidedServiceProvider.class, new ProvidedService(true) );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		assertFalse( ProvidedServiceProvider.isActive() );
		sf.close();
		assertFalse( ProvidedServiceProvider.isActive() );
	}

	@Test
	public void testServiceNotFound() throws Exception {
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration.addProperty( "hibernate.search.default.directory_provider", NoServiceDirectoryProvider.class.getName() )
				.addClass( Telephone.class );
		boolean exception = false;
		try {
			SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		}
		catch ( SearchException e ) {
			exception = true;
		}
		assertTrue( "Service not found should raise a SearchException", exception );
	}
}
