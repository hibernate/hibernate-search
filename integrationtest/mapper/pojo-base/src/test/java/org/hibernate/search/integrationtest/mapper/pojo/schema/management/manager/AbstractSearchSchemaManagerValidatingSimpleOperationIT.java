/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;

import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;

public abstract class AbstractSearchSchemaManagerValidatingSimpleOperationIT
		extends AbstractSearchSchemaManagerSimpleOperationIT {

	@Test
	void failure_single() {
		String failureMessage = "My failure";

		// We must not log the failures, see https://hibernate.atlassian.net/browse/HSEARCH-4995
		logged.expectEvent( Level.DEBUG /* or higher */, failureMessage ).never();

		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, failureCollector -> {
				failureCollector.add( failureMessage );
				return CompletableFuture.completedFuture( null );
			} );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity2.class.getName() )
							.failure( failureMessage ) );
		}
	}

	@Test
	void failure_multiple() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			expectWork( IndexedEntity1.NAME, failureCollector -> {
				failureCollector.add( "My failure 1" );
				return CompletableFuture.completedFuture( null );
			} );
			expectWork( IndexedEntity2.NAME, failureCollector -> {
				failureCollector.add( "My failure 2" );
				return CompletableFuture.completedFuture( null );
			} );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity1.class.getName() )
							.failure( "My failure 1" )
							.typeContext( IndexedEntity2.class.getName() )
							.failure( "My failure 2" ) );
		}
	}

	@Test
	void failure_exception() {
		String failureMessage = "My failure";
		String exceptionMessage = "My exception";

		// We must not log the failures, see https://hibernate.atlassian.net/browse/HSEARCH-4995
		logged.expectEvent( Level.DEBUG /* or higher */, failureMessage ).never();
		logged.expectEvent( Level.DEBUG /* or higher */, Matchers.hasToString( Matchers.containsString( exceptionMessage ) ) )
				.never();

		try ( SearchSession searchSession = mapping.createSession() ) {

			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			RuntimeException exception = new RuntimeException( exceptionMessage );
			expectWork( IndexedEntity1.NAME, failureCollector -> {
				failureCollector.add( failureMessage );
				return exceptionFuture( exception );
			} );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity1.class.getName() )
							.failure( failureMessage )
							.failure( exceptionMessage ) )
					.hasSuppressedException( exception );
		}
	}

}
