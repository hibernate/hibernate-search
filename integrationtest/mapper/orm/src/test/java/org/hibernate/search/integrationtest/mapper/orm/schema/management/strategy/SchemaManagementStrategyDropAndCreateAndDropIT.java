/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.strategy;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class SchemaManagementStrategyDropAndCreateAndDropIT extends AbstractSchemaManagementStrategyIT {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP;
	}

	@Test
	void close_drop_exception_single() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

		RuntimeException exception = new RuntimeException( "My exception" );
		expectOnClose( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectOnClose( IndexedEntity2.NAME, exceptionFuture( exception ) );

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Hibernate Search encountered failures during shutdown" )
						.withMessage( IndexedEntity2.class.getName() )
						.withSuppressed( exception )
						.build(),
				"Unable to shut down Hibernate Search"
		);
		sessionFactory.close();
		backendMock.verifyExpectationsMet();
	}

	@Test
	void close_drop_exception_multiple() {
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
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Hibernate Search encountered failures during shutdown" )
						.withMessage( IndexedEntity1.class.getName() )
						.withMessage( IndexedEntity2.class.getName() )
						.withSuppressed( exception1 )
						.withSuppressed( exception2 )
						.build(),
				"Unable to shut down Hibernate Search"
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
