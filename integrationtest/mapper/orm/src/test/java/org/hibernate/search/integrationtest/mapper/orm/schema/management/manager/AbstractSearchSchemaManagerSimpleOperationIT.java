/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.manager;

import java.util.concurrent.CompletableFuture;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractSearchSchemaManagerSimpleOperationIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected SessionFactory sessionFactory;

	@Before
	public final void setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		this.sessionFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.NONE
				)
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	@Test
	public void success_fromMapping_single() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( IndexedEntity1.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromMapping_all() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( Object.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success_fromSession_single() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSchemaManager manager = Search.session( session )
					.schemaManager( IndexedEntity1.class );
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void success_fromSession_all() {
		OrmUtils.withinSession( sessionFactory, session -> {
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
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( Object.class )
				.schemaManager();

		RuntimeException exception = new RuntimeException( "My exception" );
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, exceptionFuture( exception ) );

		Assertions.assertThatThrownBy( () -> execute( manager ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My exception" )
						.build() );
	}

	@Test
	public void exception_multiple() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( Object.class )
				.schemaManager();

		RuntimeException exception1 = new RuntimeException( "My exception 1" );
		RuntimeException exception2 = new RuntimeException( "My exception 2" );
		expectWork( IndexedEntity1.NAME, exceptionFuture( exception1 ) );
		expectWork( IndexedEntity2.NAME, exceptionFuture( exception2 ) );

		Assertions.assertThatThrownBy( () -> execute( manager ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity1.class.getName() )
						.failure( "My exception 1" )
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My exception 2" )
						.build() );
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
