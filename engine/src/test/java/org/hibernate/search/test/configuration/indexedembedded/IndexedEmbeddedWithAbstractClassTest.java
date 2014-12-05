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
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1312")
@RunWith(BMUnitRunner.class)
public class IndexedEmbeddedWithAbstractClassTest {

	@Test
	@BMRule(targetClass = "org.hibernate.search.util.logging.impl.Log_$logger",
			targetMethod = "abstractClassesCannotInsertDocuments",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testAbstractClassAnnotatedWithIndexedLogsWarning")
	public void testAbstractClassAnnotatedWithIndexedLogsWarning() {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( A.class )
				.addClass( AbstractA.class )
				.addClass( D.class );

		SearchIntegrator searchIntegrator = new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
		searchIntegrator.close();
		Assert.assertEquals( "Wrong invocation count", 1, BytemanHelper.getAndResetInvocationCount() );
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.util.logging.impl.Log_$logger",
			targetMethod = "abstractClassesCannotInsertDocuments",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testAbstractClassAnnotatedWithIndexedLogsWarning")
	public void testInvalidConfiguredPathThrowsException() {
		try {
			SearchConfigurationForTest configuration = new SearchConfigurationForTest()
					.addClass( B.class )
					.addClass( AbstractB.class )
					.addClass( D.class );

			new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
			fail( "Invalid configuration should throw an exception" );
		}
		catch (SearchException e) {
			assertTrue( "Invalid exception code", e.getMessage().startsWith( "HSEARCH000216" ) );
		}
		Assert.assertEquals( "Wrong invocation count", 0, BytemanHelper.getAndResetInvocationCount() );
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.util.logging.impl.Log_$logger",
			targetMethod = "abstractClassesCannotInsertDocuments",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testAbstractClassAnnotatedWithIndexedLogsWarning")
	public void testInvalidConfiguredPathThrowsExceptionAndIndexedAbstractClassLogsWarning() {
		try {
			SearchConfigurationForTest configuration = new SearchConfigurationForTest()
					.addClass( C.class )
					.addClass( AbstractC.class )
					.addClass( D.class );

			new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
			fail( "Invalid configuration should throw an exception" );
		}
		catch (SearchException e) {
			assertTrue( "Invalid exception code", e.getMessage().startsWith( "HSEARCH000216" ) );
		}
		Assert.assertEquals( "Wrong invocation count", 1, BytemanHelper.getAndResetInvocationCount() );
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


