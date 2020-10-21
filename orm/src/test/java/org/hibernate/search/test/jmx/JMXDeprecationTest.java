/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jmx;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;
import org.hamcrest.CoreMatchers;

@TestForIssue(jiraKey = "HSEARCH-4336")
public class JMXDeprecationTest extends SearchInitializationTestBase {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void default_warning() {
		Map<String, Object> settings = new HashMap<>();
		enableJmx( settings );

		logged.expectEvent( Level.WARN, CoreMatchers.nullValue(),
				"Enabling JMX",
				"Support for statistics retrieved through JMX and indexing triggered through JMX is going to be removed in Hibernate Search 6" );

		init( new ImmutableTestConfiguration( settings, new Class[] { IndexedEntityType.class } ) );
	}

	@Test
	public void property_noWarning() {
		Map<String, Object> settings = new HashMap<>();
		enableJmx( settings );
		settings.put( "hibernate.search.v6_migration.deprecation_warnings", "false" );

		logged.expectEvent( Level.WARN, CoreMatchers.anything(), "JMX" ).never();

		init( new ImmutableTestConfiguration( settings, new Class[] { IndexedEntityType.class } ) );
	}

	private void enableJmx(Map<String, Object> settings) {
		Path simpleJndiDir = SimpleJNDIHelper.makeTestingJndiDirectory( JMXDeprecationTest.class );
		SimpleJNDIHelper.enableSimpleJndi( settings, simpleJndiDir );
		settings.put( "hibernate.session_factory_name", "java:comp/SessionFactory" );
		settings.put( Environment.JMX_ENABLED, "true" );
	}

	@Indexed(index = "myIndex")
	public static class IndexedEntityType {
		@DocumentId
		private long id;
		@Field
		private String field;
	}

}
