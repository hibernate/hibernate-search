/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.logging.log4j.Level;
import org.hamcrest.CoreMatchers;

@TestForIssue(jiraKey = "HSEARCH-4336")
@Category(SkipOnElasticsearch.class)
public class NonExclusiveIndexUseDeprecationTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void default_warning() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedEntityType.class );
		cfg.addProperty( "hibernate.search.default.exclusive_index_use", "false" );

		logged.expectEvent( Level.WARN, CoreMatchers.nullValue(),
				"Property 'exclusive_index_use' is set to 'false' for index 'myIndex'",
				"Support for non-exclusive index use is going to be removed in Hibernate Search 6" );

		integratorResource.create( cfg );
	}

	@Test
	public void property_noWarning() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedEntityType.class );
		cfg.addProperty( "hibernate.search.myIndex.exclusive_index_use", "false" );

		cfg.addProperty( "hibernate.search.v6_migration.deprecation_warnings", "false" );

		logged.expectLevel( Level.WARN ).never();

		integratorResource.create( cfg );
	}

	@Indexed(index = "myIndex")
	public static class IndexedEntityType {
		@DocumentId
		private long id;
		@Field
		private String field;
	}

}
