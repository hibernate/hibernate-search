/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public abstract class AbstractSearchSchemaManagerSimpleOperationIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		setupContext.withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.NONE )
				.withAnnotatedTypes( IndexedEntity1.class, IndexedEntity2.class );
	}

	@Test
	public void success_fromMapping_single() {
		SearchSchemaManager manager = Search.mapping( setupHolder.sessionFactory() )
				.scope( IndexedEntity1.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromMapping_all() {
		SearchSchemaManager manager = Search.mapping( setupHolder.sessionFactory() )
				.scope( Object.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromSession_single() {
		setupHolder.runNoTransaction( session -> {
			SearchSchemaManager manager = Search.session( session )
					.schemaManager( IndexedEntity1.class );
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void success_fromSession_all() {
		setupHolder.runNoTransaction( session -> {
			SearchSchemaManager manager = Search.session( session )
					.schemaManager();
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void exception_single() {
		SearchSchemaManager manager = Search.mapping( setupHolder.sessionFactory() )
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

	@Test
	public void exception_multiple() {
		SearchSchemaManager manager = Search.mapping( setupHolder.sessionFactory() )
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

	@Entity(name = IndexedEntity1.NAME)
	@Indexed(index = IndexedEntity1.NAME)
	static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@Id
		private Integer id;
	}

	@Entity(name = IndexedEntity2.NAME)
	@Indexed(index = IndexedEntity2.NAME)
	static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@Id
		private Integer id;
	}
}
