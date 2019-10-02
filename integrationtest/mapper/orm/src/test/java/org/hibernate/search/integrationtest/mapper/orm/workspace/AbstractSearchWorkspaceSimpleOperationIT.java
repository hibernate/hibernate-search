/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.workspace;

import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractSearchWorkspaceSimpleOperationIT {

	private static final String BACKEND1_NAME = "stubBackend1";
	private static final String BACKEND2_NAME = "stubBackend2";

	@Rule
	public BackendMock backend1Mock = new BackendMock( BACKEND1_NAME );

	@Rule
	public BackendMock backend2Mock = new BackendMock( BACKEND2_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backend1Mock, backend2Mock );

	@Test
	public void async_success() {
		SessionFactory sessionFactory = setup();

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletableFuture<?> futureFromWorkspace = executeAsync( workspace );
			backend1Mock.verifyExpectationsMet();
			assertThat( futureFromWorkspace ).isPending();

			futureFromBackend.complete( new Object() );
			assertThat( futureFromWorkspace ).isSuccessful();
		} );
	}

	@Test
	public void async_failure() {
		SessionFactory sessionFactory = setup();

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			CompletableFuture<?> futureFromWorkspace = executeAsync( workspace );
			backend1Mock.verifyExpectationsMet();
			assertThat( futureFromWorkspace ).isPending();

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );
			assertThat( futureFromWorkspace ).isFailed( exception );
		} );
	}

	@Test
	public void sync_success() {
		SessionFactory sessionFactory = setup();

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			futureFromBackend.complete( new Object() );
			executeSync( workspace );
			backend1Mock.verifyExpectationsMet();
		} );
	}

	@Test
	public void sync_failure() {
		SessionFactory sessionFactory = setup();

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchWorkspace workspace = Search.session( session ).workspace( IndexedEntity1.class );

			CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
			expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

			RuntimeException exception = new RuntimeException();
			futureFromBackend.completeExceptionally( exception );

			SubTest.expectException(
					() -> executeSync( workspace )
			)
					.assertThrown()
					.isSameAs( exception );
		} );
	}

	@Test
	public void multiIndexMultiBackend() {
		SessionFactory sessionFactory = setup();

		OrmUtils.withinSession( sessionFactory, session -> {
			SearchWorkspace workspace = Search.session( session ).workspace();

			CompletableFuture<Object> future1FromBackend = new CompletableFuture<>();
			CompletableFuture<Object> future2FromBackend = new CompletableFuture<>();
			expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, future1FromBackend );
			expectWork( backend2Mock, IndexedEntity2.INDEX_NAME, future2FromBackend );

			CompletableFuture<?> futureFromWorkspace = executeAsync( workspace );
			backend1Mock.verifyExpectationsMet();
			backend2Mock.verifyExpectationsMet();
			assertThat( futureFromWorkspace ).isPending();

			future1FromBackend.complete( new Object() );
			assertThat( futureFromWorkspace ).isPending();

			future2FromBackend.complete( new Object() );
			assertThat( futureFromWorkspace ).isSuccessful();
		} );
	}

	@Test
	public void outOfSession() {
		SessionFactory sessionFactory = setup();

		SearchWorkspace workspace;
		try ( Session session = sessionFactory.openSession() ) {
			workspace = Search.session( session ).workspace( IndexedEntity1.class );
		}

		CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
		expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

		CompletableFuture<?> futureFromWorkspace = executeAsync( workspace );
		backend1Mock.verifyExpectationsMet();
		assertThat( futureFromWorkspace ).isPending();

		futureFromBackend.complete( new Object() );
		assertThat( futureFromWorkspace ).isSuccessful();
	}

	@Test
	public void fromMappingWithoutSession() {
		SessionFactory sessionFactory = setup();

		SearchWorkspace workspace = Search.mapping( sessionFactory ).scope( IndexedEntity1.class ).workspace();

		CompletableFuture<Object> futureFromBackend = new CompletableFuture<>();
		expectWork( backend1Mock, IndexedEntity1.INDEX_NAME, futureFromBackend );

		CompletableFuture<?> futureFromWriter = executeAsync( workspace );
		backend1Mock.verifyExpectationsMet();
		assertThat( futureFromWriter ).isPending();

		futureFromBackend.complete( new Object() );
		assertThat( futureFromWriter ).isSuccessful();
	}

	protected abstract void expectWork(BackendMock backendMock, String indexName, CompletableFuture<?> future);

	protected abstract void executeSync(SearchWorkspace workspace);

	protected abstract CompletableFuture<?> executeAsync(SearchWorkspace workspace);

	private SessionFactory setup() {
		backend1Mock.expectAnySchema( IndexedEntity1.INDEX_NAME );
		backend2Mock.expectAnySchema( IndexedEntity2.INDEX_NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity1.class, IndexedEntity2.class );

		backend1Mock.verifyExpectationsMet();
		backend2Mock.verifyExpectationsMet();

		return sessionFactory;
	}

	@Entity(name = "indexed1")
	@Indexed(backend = BACKEND1_NAME, index = IndexedEntity1.INDEX_NAME)
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
