/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Verifies we have appropriate error messages for wrong or legacy
 * configurations of the JGroups channel.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class JGroupsConfigurationTest {

	@Rule
	public ExpectedException error = ExpectedException.none();

	@Test
	public void refuseConfigurationFileOnDefaultIndex() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.default.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.default.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		bootConfiguration( cfg );
	}

	@Test
	public void refuseConfigurationFileOnSpecificIndex() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.dvds.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		bootConfiguration( cfg );
	}

	@Test
	public void acceptConfigurationFile() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.services.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "Error while trying to create a channel using config file: some non existing file" );
		bootConfiguration( cfg );
	}

	/**
	 * Attempts to start a SearchIntegrator, and make sure we close it if it happens to start
	 * correctly.
	 * @param cfg a configuration to try booting
	 * @throws Throwable
	 */
	private static void bootConfiguration(SearchConfigurationForTest cfg) throws Throwable {
		cfg.addClass( Dvd.class );
		SearchIntegrator searchIntegrator = null;
		try {
			searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
		}
		catch (SearchException se) {
			//we know we're getting a generic failure, but we want to make assert on the details message of
			// the cause:
			throw se.getCause();
		}
		finally {
			if ( searchIntegrator != null ) {
				searchIntegrator.close();
			}
		}
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
