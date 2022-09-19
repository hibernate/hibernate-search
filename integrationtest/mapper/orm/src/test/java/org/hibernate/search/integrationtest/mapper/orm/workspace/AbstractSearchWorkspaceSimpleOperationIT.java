/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.workspace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public abstract class AbstractSearchWorkspaceSimpleOperationIT {

	private static final String BACKEND2_NAME = "stubBackend2";

	@ClassRule
	public static BackendMock defaultBackendMock = new BackendMock();

	@ClassRule
	public static BackendMock backend2Mock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder;
	static {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND2_NAME, backend2Mock );
		setupHolder = ReusableOrmSetupHolder.withBackendMocks( defaultBackendMock, namedBackendMocks );
	}

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		defaultBackendMock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );

		setupContext.withAnnotatedTypes( IndexedEntity1.class, IndexedEntity2.class );
	}

	@Test
	public void async_success() {
		setupHolder.runNoTransaction( session -> {
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
	public void async_failure() {
		setupHolder.runNoTransaction( session -> {
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
	public void sync_success() {
		setupHolder.runNoTransaction( session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( defaultBackendMock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			futureFromBackend.complete( new Object() );
			executeSync( workspace );
			defaultBackendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void sync_failure() {
		setupHolder.runNoTransaction( session -> {
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
	public void multiIndexMultiBackend() {
		setupHolder.runNoTransaction( session -> {
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
	public void outOfSession() {
		SearchWorkspace workspace;
		try ( Session session = setupHolder.sessionFactory().openSession() ) {
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
	public void fromMappingWithoutSession() {
		SearchWorkspace workspace = Search.mapping( setupHolder.sessionFactory() )
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
