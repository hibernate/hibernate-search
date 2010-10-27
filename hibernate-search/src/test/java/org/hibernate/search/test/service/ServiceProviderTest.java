package org.hibernate.search.test.service;

import junit.framework.TestCase;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;

/**
 * @author Emmanuel Bernard
 */
public class ServiceProviderTest extends TestCase {
	public void testManagedService() throws Exception {
		MyServiceProvider.resetActive();
		assertNull( MyServiceProvider.isActive() );
		final ManualConfiguration configuration = new ManualConfiguration();
		configuration.addProperty( "hibernate.search.default.directory_provider", ServiceDirectoryProvider.class.getName() )
				.addClass( Telephone.class );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		assertTrue( MyServiceProvider.isActive() );
		sf.close();
		assertFalse( MyServiceProvider.isActive() );
	}

	public void testServiceNotFound() throws Exception {
		final ManualConfiguration configuration = new ManualConfiguration();
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
