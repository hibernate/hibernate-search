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
package org.hibernate.search.test.service;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.test.util.ManualConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
		configuration.addProperty(
				"hibernate.search.default.directory_provider",
				ServiceDirectoryProvider.class.getName()
		)
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
		configuration.addProperty(
				"hibernate.search.default.directory_provider",
				ServiceDirectoryProvider.class.getName()
		)
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
				.addProperty(
						"hibernate.search.default.directory_provider",
						ProvidedServiceDirectoryProvider.class.getName()
				)
				.addClass( Telephone.class )
				.getProvidedServices().put( ProvidedServiceProvider.class, new ProvidedService( true ) );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		assertFalse( ProvidedServiceProvider.isActive() );
		sf.close();
		assertFalse( ProvidedServiceProvider.isActive() );
	}

	@Test
	public void testServiceNotFound() throws Exception {
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration.addProperty(
				"hibernate.search.default.directory_provider",
				NoServiceDirectoryProvider.class.getName()
		)
				.addClass( Telephone.class );
		boolean exception = false;
		try {
			new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		}
		catch (SearchException e) {
			exception = true;
		}
		assertTrue( "Service not found should raise a SearchException", exception );
	}

	@Test
	public void testRequestingHibernateSessionServiceFailsWithoutORM() throws Exception {
		final ManualConfiguration configuration = new HibernateManualConfiguration();
		configuration.addProperty(
				"hibernate.search.default.directory_provider",
				DummyDirectoryProvider.class.getName()
		).addClass( Telephone.class );

		try {
			new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
			fail( "Startup should have failed due to illegal service request" );
		}
		catch (SearchException e) {
			// the exception of interest is wrapped
			Throwable expectedThrowable = e.getCause().getCause();
			assertTrue( "Unexpected message: " + expectedThrowable.getMessage(), expectedThrowable.getMessage().startsWith( "HSEARCH000190" ) );
		}
	}
}
