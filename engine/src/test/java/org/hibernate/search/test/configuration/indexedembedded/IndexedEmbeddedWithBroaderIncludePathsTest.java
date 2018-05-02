/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexedembedded;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2547")
public class IndexedEmbeddedWithBroaderIncludePathsTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBroaderIncludePathsDisallowed() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000216" );
		thrown.expectMessage( C.class.getName() );
		thrown.expectMessage( "b.a.bar" );

		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( A.class )
				.addClass( B.class )
				.addClass( C.class );

		SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
		searchIntegrator.close();
	}

	@Indexed
	private static class A {
		@DocumentId
		private Long id;

		@Field(analyze = Analyze.NO)
		private String foo;

		@Field(analyze = Analyze.NO)
		private String bar;
	}

	@Indexed
	private static class B {
		@DocumentId
		private Long id;

		@IndexedEmbedded(includePaths = "foo") // Include only "a.foo"
		private A a;
	}

	@Indexed
	private static class C {
		@DocumentId
		private Long id;

		@IndexedEmbedded(includePaths = { "a.foo", "a.bar" }) // Try to include "b.a.bar": this should not work, since "a.bar" is not included in b
		private B b;
	}
}


