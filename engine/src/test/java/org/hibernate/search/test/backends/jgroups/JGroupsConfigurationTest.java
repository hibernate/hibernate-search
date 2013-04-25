/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.backends.jgroups;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.default.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		bootConfiguration( cfg );
	}

	@Test
	public void refuseConfigurationFileOnSpecificIndex() throws Throwable {
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.dvds.worker.backend.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "JGroups channel configuration should be specified in the global section" );
		bootConfiguration( cfg );
	}

	@Test
	public void acceptConfigurationFile() throws Throwable {
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.dvds.worker.backend", "jgroupsMaster" )
			.addProperty( "hibernate.search.services.jgroups.configurationFile", "some non existing file" )
			;
		error.expect( SearchException.class );
		error.expectMessage( "Error while trying to create a channel using config file: some non existing file" );
		bootConfiguration( cfg );
	}

	/**
	 * Attempts to start a SearchFactory, and make sure we close it if it happens to start
	 * correctly.
	 * @param cfg a configuration to try booting
	 * @throws Throwable
	 */
	private static void bootConfiguration(ManualConfiguration cfg) throws Throwable {
		cfg.addClass( Dvd.class );
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		SearchFactoryImplementor buildSearchFactory = null;
		try {
			buildSearchFactory = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		}
		catch (SearchException se) {
			//we know we're getting a generic failure, but we want to make assert on the details message of
			// the cause:
			throw se.getCause();
		}
		finally {
			if ( buildSearchFactory != null ) {
				buildSearchFactory.close();
			}
		}
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
