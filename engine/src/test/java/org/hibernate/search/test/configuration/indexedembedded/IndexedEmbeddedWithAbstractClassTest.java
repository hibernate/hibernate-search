/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexedembedded;

import java.util.List;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1312")
public class IndexedEmbeddedWithAbstractClassTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void testAbstractClassAnnotatedWithIndexedLogsWarning() {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( A.class )
				.addClass( AbstractA.class )
				.addClass( D.class );

		logged.expectMessage( "HSEARCH000044", "@Indexed", AbstractA.class.getName() );

		integratorResource.create( configuration );
	}

	@Test
	public void testInvalidConfiguredPathThrowsException() {
		try {
			SearchConfigurationForTest configuration = new SearchConfigurationForTest()
					.addClass( B.class )
					.addClass( AbstractB.class )
					.addClass( D.class );

			logged.expectMessageMissing( "HSEARCH000044", "@Indexed", AbstractB.class.getName() );

			integratorResource.create( configuration );
			fail( "Invalid configuration should throw an exception" );
		}
		catch (SearchException e) {
			assertTrue( "Invalid exception code", e.getMessage().startsWith( "HSEARCH000216" ) );
		}
	}

	@Test
	public void testInvalidConfiguredPathThrowsExceptionAndIndexedAbstractClassLogsWarning() {
		try {
			SearchConfigurationForTest configuration = new SearchConfigurationForTest()
					.addClass( C.class )
					.addClass( AbstractC.class )
					.addClass( D.class );

			logged.expectMessage( "HSEARCH000044", "@Indexed", AbstractC.class.getName() );

			integratorResource.create( configuration );
			fail( "Invalid configuration should throw an exception" );
		}
		catch (SearchException e) {
			assertTrue( "Invalid exception code", e.getMessage().startsWith( "HSEARCH000216" ) );
		}
	}

	@Indexed
	public abstract static class AbstractA {

		@DocumentId
		long id;

		@IndexedEmbedded(includePaths = { "foo" })
		List<D> list;
	}

	@Indexed
	public static final class A extends AbstractA {
	}

	public abstract static class AbstractB {

		@DocumentId
		long id;

		@IndexedEmbedded(includePaths = { "snafu", "fubar" })
		List<D> list;
	}

	@Indexed
	public static final class B extends AbstractB {
	}

	@Indexed
	public abstract static class AbstractC {

		@DocumentId
		long id;

		@IndexedEmbedded(includePaths = { "snafu", "fubar" })
		List<D> list;
	}

	@Indexed
	public static final class C extends AbstractC {
	}

	@Indexed
	public static class D {

		@DocumentId
		long id;

		@Field
		String foo;
	}
}


