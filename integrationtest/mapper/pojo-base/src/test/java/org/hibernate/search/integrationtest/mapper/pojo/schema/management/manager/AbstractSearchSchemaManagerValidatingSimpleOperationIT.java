/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;

import org.junit.Test;

public abstract class AbstractSearchSchemaManagerValidatingSimpleOperationIT
		extends AbstractSearchSchemaManagerSimpleOperationIT {

	@Test
	public void failure_single() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, failureCollector -> {
				failureCollector.add( "My failure" );
				return CompletableFuture.completedFuture( null );
			} );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity2.class.getName() )
							.failure( "My failure" ) );
		}
	}

	@Test
	public void failure_multiple() {
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
	public void failure_exception() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			RuntimeException exception = new RuntimeException( "My exception" );
			expectWork( IndexedEntity1.NAME, failureCollector -> {
				failureCollector.add( "My failure" );
				return exceptionFuture( exception );
			} );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity1.class.getName() )
							.failure( "My failure" )
							.failure( "My exception" ) );
		}
	}

}
