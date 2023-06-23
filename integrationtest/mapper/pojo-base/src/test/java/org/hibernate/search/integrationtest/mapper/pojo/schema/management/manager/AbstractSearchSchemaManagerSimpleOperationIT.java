/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractSearchSchemaManagerSimpleOperationIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );

		mapping = setupHelper.start()
				.setup( IndexedEntity1.class, IndexedEntity2.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromMapping_single() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( IndexedEntity1.class )
					.schemaManager();
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromMapping_all() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromSession_single() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.schemaManager( IndexedEntity1.class );
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromSession_all() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.schemaManager();
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void exception_single() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			RuntimeException exception = new RuntimeException( "My exception" );
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, exceptionFuture( exception ) );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity2.class.getName() )
							.failure( "My exception" ) );
		}
	}

	@Test
	public void exception_multiple() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchSchemaManager manager = searchSession
					.scope( Object.class )
					.schemaManager();

			RuntimeException exception1 = new RuntimeException( "My exception 1" );
			RuntimeException exception2 = new RuntimeException( "My exception 2" );
			expectWork( IndexedEntity1.NAME, exceptionFuture( exception1 ) );
			expectWork( IndexedEntity2.NAME, exceptionFuture( exception2 ) );

			assertThatThrownBy( () -> execute( manager ) )
					.isInstanceOf( SearchException.class )
					.satisfies( FailureReportUtils.hasFailureReport()
							.typeContext( IndexedEntity1.class.getName() )
							.failure( "My exception 1" )
							.typeContext( IndexedEntity2.class.getName() )
							.failure( "My exception 2" ) );
		}
	}

	protected abstract void execute(SearchSchemaManager manager);

	protected final void expectWork(String indexName, CompletableFuture<?> future) {
		expectWork( indexName, ignored -> future );
	}

	protected abstract void expectWork(String indexName, SchemaManagementWorkBehavior behavior);

	protected abstract void expectOnClose(String indexName);

	protected final CompletableFuture<?> exceptionFuture(RuntimeException exception) {
		CompletableFuture<?> future = new CompletableFuture<>();
		future.completeExceptionally( exception );
		return future;
	}

	@Indexed(index = IndexedEntity1.NAME)
	static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@DocumentId
		private Integer id;
	}

	@Indexed(index = IndexedEntity2.NAME)
	static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@DocumentId
		private Integer id;
	}
}
