/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Verifies we have appropriate error messages for wrong or legacy
 * configurations of the JGroups channel.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class JGroupsConfigurationTest {

	@Rule
	public ExpectedException error = ExpectedException.none();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void refuseConfigurationFileOnDefaultIndex() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.default.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.default.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		init( cfg );
	}

	@Test
	public void refuseConfigurationFileOnSpecificIndex() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.dvds.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		init( cfg );
	}

	@Test
	public void acceptConfigurationFile() throws Throwable {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.services.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "Error while trying to create a channel using config file: some non existing file" );
		init( cfg );
	}

	private void init(SearchConfigurationForTest cfg) throws Throwable {
		cfg.addClass( Dvd.class );
		integratorResource.create( cfg );
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
