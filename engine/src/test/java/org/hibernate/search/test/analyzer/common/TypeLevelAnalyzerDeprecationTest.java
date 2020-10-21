/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.hamcrest.CoreMatchers;

@TestForIssue(jiraKey = "HSEARCH-4336")
public class TypeLevelAnalyzerDeprecationTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void default_warning() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedEntityType.class );

		logged.expectEvent( Level.WARN, CoreMatchers.nullValue(),
				"Type '" + IndexedEntityType.class.getName() + "' is annotated with @Analyzer",
				"Support for @Analyzer on types is going to be removed in Hibernate Search 6" );

		integratorResource.create( cfg );
	}

	@Test
	public void property_noWarning() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedEntityType.class );

		cfg.addProperty( "hibernate.search.v6_migration.deprecation_warnings", "false" );

		logged.expectLevel( Level.WARN ).never();

		integratorResource.create( cfg );
	}

	@Indexed(index = "myIndex")
	@Analyzer(impl = WhitespaceAnalyzer.class)
	public static class IndexedEntityType {
		@DocumentId
		private long id;
		@Field
		private String field;
	}

}
