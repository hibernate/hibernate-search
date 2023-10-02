/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;

import org.junit.jupiter.api.Test;

public abstract class AbstractSchemaManagementStrategyValidatingIT extends AbstractSchemaManagementStrategyIT {

	@Test
	void failure_single() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, failureCollector -> {
			failureCollector.add( "My failure" );
			return CompletableFuture.completedFuture( null );
		} );

		assertThatThrownBy( this::setup )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My failure" ) );
	}

	@Test
	void failure_multiple() {
		expectWork( IndexedEntity1.NAME, failureCollector -> {
			failureCollector.add( "My failure 1" );
			return CompletableFuture.completedFuture( null );
		} );
		expectWork( IndexedEntity2.NAME, failureCollector -> {
			failureCollector.add( "My failure 2" );
			return CompletableFuture.completedFuture( null );
		} );

		assertThatThrownBy( this::setup )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity1.class.getName() )
						.failure( "My failure 1" )
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My failure 2" ) );
	}

	@Test
	void failure_exception() {
		RuntimeException exception = new RuntimeException( "My exception" );
		expectWork( IndexedEntity1.NAME, failureCollector -> {
			failureCollector.add( "My failure" );
			return exceptionFuture( exception );
		} );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

		assertThatThrownBy( this::setup )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity1.class.getName() )
						.failure( "My failure" )
						.failure( "My exception" ) );
	}

}
