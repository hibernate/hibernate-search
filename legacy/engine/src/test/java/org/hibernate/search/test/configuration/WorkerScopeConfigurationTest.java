/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;
import java.util.Properties;

import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.backend.impl.PerTransactionWorker;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.WorkerBuildContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests to verify worker scope options.
 *
 * @author Hardy Ferentschik
 */
public class WorkerScopeConfigurationTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchConfigurationForTest manualConfiguration;

	@Before
	public void setUp() {
		manualConfiguration = new SearchConfigurationForTest();
		SearchMapping searchMapping = new SearchMapping();
		searchMapping.entity( Document.class ).indexed()
				.property( "id", ElementType.FIELD ).documentId()
				.property( "title", ElementType.FIELD ).field();
		manualConfiguration.setProgrammaticMapping( searchMapping );
		manualConfiguration.addClass( Document.class );
	}

	@Test
	public void testDefaultWorker() {
		SearchIntegrator searchIntegrator = integratorResource.create( manualConfiguration );
		assertNotNull( "Worker should have been created", searchIntegrator.getWorker() );
		assertTrue( "Wrong worker class", searchIntegrator.getWorker() instanceof PerTransactionWorker );
	}

	@Test
	public void testExplicitTransactionalWorker() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", "transaction" );
		SearchIntegrator searchIntegrator = integratorResource.create( manualConfiguration );
		assertNotNull( "Worker should have been created", searchIntegrator.getWorker() );
		assertTrue( "Wrong worker class", searchIntegrator.getWorker() instanceof PerTransactionWorker );
	}

	@Test
	public void testCustomWorker() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", CustomWorker.class.getName() );
		SearchIntegrator searchIntegrator = integratorResource.create( manualConfiguration );
		assertNotNull( "Worker should have been created", searchIntegrator.getWorker() );
		assertTrue( "Wrong worker class", searchIntegrator.getWorker() instanceof CustomWorker );
	}

	@Test
	public void testCustomWorkerWithProperties() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", CustomWorkerExpectingFooAndBar.class.getName() );
		manualConfiguration.addProperty( "hibernate.search.worker.foo", "foo" );
		manualConfiguration.addProperty( "hibernate.search.worker.bar", "bar" );
		SearchIntegrator searchIntegrator = integratorResource.create( manualConfiguration );
		assertNotNull( "Worker should have been created", searchIntegrator.getWorker() );
		assertTrue( "Wrong worker class", searchIntegrator.getWorker() instanceof CustomWorkerExpectingFooAndBar );
	}

	@Test
	public void testUnknownWorkerImplementationClass() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", "foo" );
		try {
			integratorResource.create( manualConfiguration );
			fail();
		}
		catch (SearchException e) {
			assertTrue(
					"Unexpected error message",
					e.getMessage().contains( "Unable to find worker implementation class: foo" )
			);
		}
	}

	@SuppressWarnings("unused")
	public static final class Document {

		private long id;
		private String title;

	}

	public static final class CustomWorker implements Worker {
		@Override
		public void performWork(Work work, TransactionContext transactionContext) {
		}

		@Override
		public void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor) {
		}

		@Override
		public void close() {
		}

		@Override
		public void flushWorks(TransactionContext transactionContext) {
		}
	}

	public static final class CustomWorkerExpectingFooAndBar implements Worker {
		public static final String FOO = "hibernate.search.worker.foo";
		public static final String BAR = "hibernate.search.worker.bar";

		@Override
		public void performWork(Work work, TransactionContext transactionContext) {
		}

		@Override
		public void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor) {
			assertTrue( "Missing property: " + FOO, props.containsKey( FOO ) );
			assertTrue( "Missing property: " + BAR, props.containsKey( BAR ) );
		}

		@Override
		public void close() {
		}

		@Override
		public void flushWorks(TransactionContext transactionContext) {
		}
	}
}
