/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.workspace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractSearchWorkspaceSimpleOperationIT {

	private static final String BACKEND2_NAME = "stubBackend2";

	@RegisterExtension
	public static BackendMock defaultBackendMock = BackendMock.create();

	@RegisterExtension
	public static BackendMock backend2Mock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withBackendMocks( defaultBackendMock, Collections.singletonMap( BACKEND2_NAME, backend2Mock ) );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		defaultBackendMock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );
		sessionFactory = ormSetupHelper.start()
				.withAnnotatedTypes( IndexedEntity1.class, IndexedEntity2.class )
				.setup();
	}

	@Test
	void async_success() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
			defaultBackendMock.verifyExpectationsMet();
			assertThatFuture( futureFromWorkspace ).isPending();

			futureFromBackend.complete( new Object() );
			assertThatFuture( futureFromWorkspace ).isSuccessful();
		} );
	}

	@Test
	void async_failure() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
			defaultBackendMock.verifyExpectationsMet();
			assertThatFuture( futureFromWorkspace ).isPending();

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );
			assertThatFuture( futureFromWorkspace ).isFailed( exception );
		} );
	}

	@Test
	void sync_success() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			futureFromBackend.complete( new Object() );
			executeSync( workspace );
			defaultBackendMock.verifyExpectationsMet();
		} );
	}

	@Test
	void sync_failure() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );

			assertThatThrownBy(
					() -> executeSync( workspace )
			)
					.isSameAs( exception );
		} );
	}

	@Test
	void multiIndexMultiBackend() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace();

			CompletableFuture<Object> future1FromBackend = new CompletableFuture<>();
			CompletableFuture<Object> future2FromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, future1FromBackend );
			expectWork( backend2Mock, IndexedEntity2.INDEX_NAME, future2FromBackend );

			CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
			defaultBackendMock.verifyExpectationsMet();
			backend2Mock.verifyExpectationsMet();
			assertThatFuture( futureFromWorkspace ).isPending();

			future1FromBackend.complete( new Object() );
			assertThatFuture( futureFromWorkspace ).isPending();

			future2FromBackend.complete( new Object() );
			assertThatFuture( futureFromWorkspace ).isSuccessful();
		} );
	}

	@Test
	void outOfSession() {
		SearchWorkspace workspace;
		try ( Session session = sessionFactory.openSession() ) {
			workspace = Search.session( session ).workspace( IndexedEntity1.class );
		}

		CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
		expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

		CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
		defaultBackendMock.verifyExpectationsMet();
		assertThatFuture( futureFromWorkspace ).isPending();

		futureFromBackend.complete( new Object() );
		assertThatFuture( futureFromWorkspace ).isSuccessful();
	}

	@Test
	void fromMappingWithoutSession() {
		SearchWorkspace workspace = Search.mapping( sessionFactory )
				.scope( IndexedEntity1.class ).workspace();

		CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
		expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

		CompletionStage<?> futureFromWriter = executeAsync( workspace );
		defaultBackendMock.verifyExpectationsMet();
		assertThatFuture( futureFromWriter ).isPending();

		futureFromBackend.complete( new Object() );
		assertThatFuture( futureFromWriter ).isSuccessful();
	}

	protected abstract void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future);

	protected abstract void executeSync(SearchWorkspace workspace);

	protected abstract CompletionStage<?> executeAsync(SearchWorkspace workspace);

	@Entity(name = "indexed1")
	@Indexed(index = IndexedEntity1.INDEX_NAME)
	public static class IndexedEntity1 {

		static final String INDEX_NAME = "index1Name";

		@Id
		private Integer id;
	}

	@Entity(name = "indexed2")
	@Indexed(backend = BACKEND2_NAME, index = IndexedEntity2.INDEX_NAME)
	public static class IndexedEntity2 {

		static final String INDEX_NAME = "index2Name";

		@Id
		private Integer id;
	}
}
