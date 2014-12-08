/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan;

import java.io.IOException;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.infinispan.impl.DefaultCacheManagerService;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.spi.SearchIntegratorBuilder;
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
		new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
