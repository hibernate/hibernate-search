/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.infinispan;

import java.io.IOException;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Test to verify the configuration property {@link DefaultCacheManagerService#INFINISPAN_TRANSPORT_OVERRIDE_RESOURCENAME}
 * is not ignored.
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
@TestForIssue(jiraKey = "HSEARCH-1575")
public class OverrideConfigurationAppliedTest {

	@Rule
	public ExpectedException exceptions = ExpectedException.none();

	@Test
	public void testOverrideOptionGetsApplied() throws IOException {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.addProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.addProperty( DefaultCacheManagerService.INFINISPAN_TRANSPORT_OVERRIDE_RESOURCENAME, "not existing" )
			.addClass( Dvd.class );

		//The most practical way to figure out if the property was applied is to provide it with
		//an illegal value to then verify the failure.
		exceptions.expect( SearchException.class );
		exceptions.expectMessage( "HSEARCH000103" );
		new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
	}

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
