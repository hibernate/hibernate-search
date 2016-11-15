/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

/**
 * Test to make sure that the ES host can be configured and the default is not applied unconditionally. To test it, the
 * host is set to a non-available URL, and the assertion is based on the resulting exception.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-2274")
public class HostCanBeConfiguredIT extends SearchInitializationTestBase {

	@Test
	public void shouldApplyConfiguredSingleElasticsearchHost() {
		try {
			init( "http://localhost:9201" );
			fail( "Expecting exception due to unavailable ES host" );
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).startsWith( "HSEARCH400007" );

			/*
			 * Initialization should fail when probing for the index (first request).
			 */
			assertThat( e.getMessage() ).contains( "Operation: IndicesExist" );

			Throwable rootCause = getRootCause( e );
			assertThat( rootCause ).isInstanceOf( ConnectException.class );
			//N.B. the exact message is different on Windows vs Linux so
			//we only check for it to contain this string:
			assertThat( rootCause.getMessage() ).contains( "Connection refused" );
		}
	}

	@Test
	public void shouldApplyConfiguredMultipleElasticsearchHosts() {
		try {
			init( " http://localhost:9200 http://localhost:9201" );
			fail( "Expecting exception due to unavailable ES host" );
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).startsWith( "HSEARCH400007" );

			/*
			 * Since the first host is valid, Initialization should not fail when probing
			 * for the index (first request), but when creating the index (second request).
			 */
			assertThat( e.getMessage() ).contains( "Operation: CreateIndex" );

			Throwable rootCause = getRootCause( e );
			assertThat( rootCause ).isInstanceOf( ConnectException.class );
			//N.B. the exact message is different on Windows vs Linux so
			//we only check for it to contain this string:
			assertThat( rootCause.getMessage() ).contains( "Connection refused" );
		}
	}

	private void init(String hosts) {
		Map<String, Object> settings = new HashMap<>();

		settings.put( "hibernate.search.default.elasticsearch.host", hosts );

		settings.put( "hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.RECREATE_DELETE.name() );

		init( new ImmutableTestConfiguration( settings, new Class<?>[]{ MasterThesis.class } ) );
	}

	private Throwable getRootCause(Throwable throwable) {
		while ( true ) {
			Throwable cause = throwable.getCause();
			if ( cause == null ) {
				return throwable;
			}
			throwable = cause;
		}
	}
}
