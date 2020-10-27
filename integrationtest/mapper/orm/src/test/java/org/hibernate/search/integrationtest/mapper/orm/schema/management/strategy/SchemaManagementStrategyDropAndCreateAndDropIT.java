/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;

public class SchemaManagementStrategyDropAndCreateAndDropIT extends AbstractSchemaManagementStrategyIT {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP;
	}

	@Test
	public void close_drop_exception_single() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

		RuntimeException exception = new RuntimeException( "My exception" );
		expectOnClose( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectOnClose( IndexedEntity2.NAME, exceptionFuture( exception ) );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( exception ).build(),
				"Hibernate Search encountered a failure during shutdown",
				IndexedEntity2.class.getName()
		);
		sessionFactory.close();
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void close_drop_exception_multiple() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

		RuntimeException exception1 = new RuntimeException( "My exception 1" );
		RuntimeException exception2 = new RuntimeException( "My exception 2" );
		expectOnClose( IndexedEntity1.NAME, exceptionFuture( exception1 ) );
		expectOnClose( IndexedEntity2.NAME, exceptionFuture( exception2 ) );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( exception1 ).build(),
				"Hibernate Search encountered a failure during shutdown",
				IndexedEntity1.class.getName()
		);
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( exception2 ).build(),
				"Hibernate Search encountered a failure during shutdown",
				IndexedEntity2.class.getName()
		);
		sessionFactory.close();
		backendMock.verifyExpectationsMet();
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_AND_CREATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING );
	}

	protected void expectOnClose(String indexName, CompletableFuture<?> future) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING, future );
	}
}
