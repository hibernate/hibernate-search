/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.workspace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractSearchWorkspaceSimpleOperationIT {

	private static final String BACKEND2_NAME = "stubBackend2";

	@RegisterExtension
	public BackendMock defaultBackendMock = BackendMock.create();

	@RegisterExtension
	public BackendMock backend2Mock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper;

	private SearchMapping mapping;

	public AbstractSearchWorkspaceSimpleOperationIT() {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND2_NAME, backend2Mock );
		setupHelper = StandalonePojoMappingSetupHelper.withBackendMocks( MethodHandles.lookup(),
				defaultBackendMock, namedBackendMocks );
	}

	@BeforeEach
	void setup() {
		defaultBackendMock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );

		mapping = setupHelper.start()
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	@Test
	void async_success() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchWorkspace workspace = session.workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
			defaultBackendMock.verifyExpectationsMet();
			assertThatFuture( futureFromWorkspace ).isPending();

			futureFromBackend.complete( new Object() );
			assertThatFuture( futureFromWorkspace ).isSuccessful();
		}
	}

	@Test
	void async_failure() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchWorkspace workspace = session.workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletionStage<?> futureFromWorkspace = executeAsync( workspace );
			defaultBackendMock.verifyExpectationsMet();
			assertThatFuture( futureFromWorkspace ).isPending();

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );
			assertThatFuture( futureFromWorkspace ).isFailed( exception );
		}
	}

	@Test
	void sync_success() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchWorkspace workspace = session.workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			futureFromBackend.complete( new Object() );
			executeSync( workspace );
			defaultBackendMock.verifyExpectationsMet();
		}
	}

	@Test
	void sync_failure() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchWorkspace workspace = session.workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );

			assertThatThrownBy(
					() -> executeSync( workspace )
			)
					.isSameAs( exception );
		}
	}

	@Test
	void multiIndexMultiBackend() {
		try ( SearchSession session = mapping.createSession() ) {
			SearchWorkspace workspace = session.workspace();

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
		}
	}

	@Test
	void outOfSession() {
		SearchWorkspace workspace;
		try ( SearchSession session = mapping.createSession() ) {
			workspace = session.workspace( IndexedEntity1.class );
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
		SearchWorkspace workspace = mapping
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

	@Indexed(index = IndexedEntity1.INDEX_NAME)
	public static class IndexedEntity1 {

		static final String INDEX_NAME = "index1Name";

		@DocumentId
		private Integer id;
	}

	@Indexed(backend = BACKEND2_NAME, index = IndexedEntity2.INDEX_NAME)
	public static class IndexedEntity2 {

		static final String INDEX_NAME = "index2Name";

		@DocumentId
		private Integer id;
	}
}
