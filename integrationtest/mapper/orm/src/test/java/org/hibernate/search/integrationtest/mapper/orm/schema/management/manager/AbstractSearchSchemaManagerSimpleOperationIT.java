/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.schema.management.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.CompletableFuture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractSearchSchemaManagerSimpleOperationIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	protected SessionFactory sessionFactory;

	@RegisterExtension
	final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, SchemaManagementStrategyName.NONE )
				.withAnnotatedTypes( IndexedEntity1.class, IndexedEntity2.class )
				.setup();
	}

	@Test
	void success_fromMapping_single() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( IndexedEntity1.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void success_fromMapping_all() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( Object.class )
				.schemaManager();
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
		execute( manager );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void success_fromSession_single() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSchemaManager manager = Search.session( session )
					.schemaManager( IndexedEntity1.class );
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	void success_fromSession_all() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSchemaManager manager = Search.session( session )
					.schemaManager();
			expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
			expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
			execute( manager );
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	void exception_single() {
		String exceptionMessage = "My exception";

		// We must not log the exceptions, see https://hibernate.atlassian.net/browse/HSEARCH-4995
		logged.expectEvent( Level.DEBUG,
				Matchers.hasToString( Matchers.containsString( exceptionMessage ) ) )
				.never();

		SearchSchemaManager manager = Search.mapping( sessionFactory )
				.scope( Object.class )
				.schemaManager();

		RuntimeException exception = new RuntimeException( exceptionMessage );
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, exceptionFuture( exception ) );

		assertThatThrownBy( () -> execute( manager ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity2.class.getName() )
						.failure( exceptionMessage ) )
				.hasSuppressedException( exception );
	}

	@Test
	void exception_multiple() {
		SearchSchemaManager manager = Search.mapping( sessionFactory )
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
