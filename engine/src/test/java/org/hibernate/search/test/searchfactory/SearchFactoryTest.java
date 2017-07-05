/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.searchfactory;

import static java.lang.annotation.ElementType.FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.hibernate.search.testsupport.BytemanHelper.SimulatedFailureException;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class SearchFactoryTest {

	@Rule
	public BytemanAccessor bytemanAccessor = BytemanHelper.createAccessor();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testTypeWithNoDocumentIdThrowsException() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
		;
		cfg.setProgrammaticMapping( mapping );

		try {
			integratorResource.create( cfg );
			fail( "Invalid configuration should have thrown an exception" );
		}
		catch (SearchException e) {
			assertTrue( e.getMessage().startsWith( "HSEARCH000177" ) );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2277")
	@BMRules(rules = {
			@BMRule(
					name = "Simulate failure on index manager initialization",
					isInterface = true,
					targetClass = "org.hibernate.search.indexes.spi.IndexManager",
					targetMethod = "initialize(String, Properties, Similarity, WorkerBuildContext)",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "simulateFailure()"
			),
			@BMRule(
					name = "Track calls to ServiceManager.releaseAllServices",
					targetClass = "org.hibernate.search.engine.service.impl.StandardServiceManager",
					targetMethod = "releaseAllServices()",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()"
			)
	} )
	public void testServicesStoppedAfterIndexInitializationException() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addClass( AnnotatedClass.class ).addClass( SecondAnnotatedClass.class );

		try {
			integratorResource.create( cfg );
			failBecauseBytemanRulesDidNotWork();
		}
		catch (SearchException e) {
			assertEquals( 1, bytemanAccessor.getAndResetInvocationCount() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2277")
	@BMRules(rules = {
			@BMRule(
					name = "Skip failure on second index manager initialization",
					isInterface = true,
					targetClass = "org.hibernate.search.indexes.spi.IndexManager",
					targetMethod = "initialize(String, Properties, Similarity, WorkerBuildContext)",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					// Only fail after at least one index manager was successfully initialized
					condition = "incrementCounter(\"indexManagerInitialize\") >= 2",
					action = "simulateFailure()"
			),
			@BMRule(
					name = "Track calls to IndexManager.destroy",
					isInterface = true,
					targetClass = "org.hibernate.search.indexes.spi.IndexManager",
					targetMethod = "destroy()",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()"
			)
	} )
	public void testIndexManagerStoppedAfterIndexInitializationException() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addClass( AnnotatedClass.class ).addClass( SecondAnnotatedClass.class );

		try {
			integratorResource.create( cfg );
			failBecauseBytemanRulesDidNotWork();
		}
		catch (SearchException e) {
			/*
			 * Expect only one index manager to be cleaned up
			 * (the initialization failed for the other one, so we shouldn't attempt to clean it up)
			 */
			assertEquals( 1, bytemanAccessor.getAndResetInvocationCount() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2277")
	@BMRules(rules = {
			@BMRule(
					name = "Simulate failure on index manager search factory setup",
					targetClass = "org.hibernate.search.engine.impl.MutableSearchFactoryState",
					targetMethod = "setActiveSearchIntegrator(ExtendedSearchIntegratorWithShareableState)",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "simulateFailure()"
			),
			@BMRule(
					name = "Track calls to ServiceManager.releaseAllServices",
					targetClass = "org.hibernate.search.engine.service.impl.StandardServiceManager",
					targetMethod = "releaseAllServices()",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()"
			)
	} )
	public void testServicesStoppedAfterIndexManagerSearchFactorySetupException() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest().addClass( AnnotatedClass.class );

		try {
			integratorResource.create( cfg );
			failBecauseBytemanRulesDidNotWork();
		}
		catch (SimulatedFailureException e) {
			assertEquals( 1, bytemanAccessor.getAndResetInvocationCount() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2277")
	@BMRules(rules = {
			@BMRule(
					name = "Simulate failure on index manager search factory setup",
					targetClass = "org.hibernate.search.engine.impl.MutableSearchFactoryState",
					targetMethod = "setActiveSearchIntegrator(ExtendedSearchIntegratorWithShareableState)",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "simulateFailure()"
			),
			@BMRule(
					name = "Track calls to IndexManager.destroy",
					isInterface = true,
					targetClass = "org.hibernate.search.indexes.spi.IndexManager",
					targetMethod = "destroy()",
					helper = "org.hibernate.search.testsupport.BytemanHelper",
					action = "countInvocation()"
			)
	} )
	public void testIndexManagerStoppedAfterIndexManagerSearchFactorySetupException() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addClass( AnnotatedClass.class ).addClass( SecondAnnotatedClass.class );

		try {
			integratorResource.create( cfg );
			failBecauseBytemanRulesDidNotWork();
		}
		catch (SimulatedFailureException e) {
			// Expect both index managers to be cleaned up
			assertEquals( 2, bytemanAccessor.getAndResetInvocationCount() );
		}
	}

	@Test
	public void testGetIndexedTypesNoTypeIndexed() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchIntegrator si = integratorResource.create( cfg );
		IndexedTypeSet indexedClasses = si.getIndexedTypeIdentifiers();
		assertEquals( "Wrong number of indexed entities", 0, indexedClasses.size() );
	}

	@Test
	public void testGetIndexedTypeSingleIndexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchIntegrator si = integratorResource.create( cfg );
		IndexedTypeSet indexedClasses = si.getIndexedTypeIdentifiers();
		assertEquals( "Wrong number of indexed entities", 1, indexedClasses.size() );
		assertTrue( indexedClasses.iterator().next().equals( new PojoIndexedTypeIdentifier( Foo.class ) ) );
	}

	@Test
	public void testGetIndexedTypesMultipleTypes() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
				.entity( Bar.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchIntegrator si = integratorResource.create( cfg );
		IndexedTypeSet indexedClasses = si.getIndexedTypeIdentifiers();
		assertEquals( "Wrong number of indexed entities", 2, indexedClasses.size() );
	}

	@Test
	public void testGetTypeDescriptorForUnindexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchIntegrator si = integratorResource.create( cfg );
		IndexedTypeDescriptor indexedTypeDescriptor = si.getIndexedTypeDescriptor( new PojoIndexedTypeIdentifier( Foo.class ) );
		assertNotNull( indexedTypeDescriptor );
		assertFalse( indexedTypeDescriptor.isIndexed() );
	}

	@Test
	public void testGetTypeDescriptorForIndexedType() {
		SearchConfigurationForTest cfg = getManualConfiguration();

		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Foo.class ).indexed()
				.property( "id", FIELD ).documentId()
		;
		cfg.setProgrammaticMapping( mapping );

		SearchIntegrator si = integratorResource.create( cfg );
		IndexedTypeDescriptor indexedTypeDescriptor = si.getIndexedTypeDescriptor( new PojoIndexedTypeIdentifier( Foo.class ) );
		assertNotNull( indexedTypeDescriptor );
		assertTrue( indexedTypeDescriptor.isIndexed() );
	}

	private SearchConfigurationForTest getManualConfiguration() {
		return new SearchConfigurationForTest()
			.addClass( Foo.class )
			.addClass( Bar.class );
	}

	private void failBecauseBytemanRulesDidNotWork() {
		fail( "The test could not run because some ByteMan rule did not execute properly." );
	}

	public static class Foo {
		private long id;
	}

	public static class Bar {
		private long id;
	}

	@Indexed
	private static class AnnotatedClass {
		@DocumentId
		private long id;

		@Field
		private String field;
	}

	@Indexed
	private static class SecondAnnotatedClass {
		@DocumentId
		private long id;

		@Field
		private String field;
	}
}


