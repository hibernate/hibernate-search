/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.ConnectException;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

/**
 * Test to make sure that the ES host can be configured and not the default is applied unconditionally. To test it, the
 * host is set to a non-available URL, and the assertion is based on the resulting exception.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-2274")
public class HostCanBeConfiguredIT {

	@Test
	public void shouldApplyConfiguredElasticsearchHost() {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.search.default.elasticsearch.host", "http://localhost:9201" );

		Metadata metadata = new MetadataSources( srb.build() )
				.addAnnotatedClass( MasterThesis.class )
				.buildMetadata();

		try {
			metadata.buildSessionFactory();
			fail( "Expecting exception due to unavailable ES host" );
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).startsWith( "HSEARCH400007" );

			Throwable rootCause = getRootCause( e );
			assertThat( rootCause ).isInstanceOf( ConnectException.class );
			assertThat( rootCause ).hasMessage( "Connection refused" );
		}

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
