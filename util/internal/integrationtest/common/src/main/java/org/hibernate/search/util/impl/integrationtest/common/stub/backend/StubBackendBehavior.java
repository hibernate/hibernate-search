/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

public abstract class StubBackendBehavior {

	private static final StubBackendBehavior DEFAULT = new StubBackendBehavior() {
		@Override
		public void onAddField(String indexName, String absoluteFieldPath) {
			throw new IllegalStateException( "The stub backend behavior was not set when a field was added to index '"
					+ indexName + "': " + absoluteFieldPath );
		}

		@Override
		public void pushSchema(String indexName, StubIndexSchemaNode rootSchemaNode) {
			throw new IllegalStateException( "The stub backend behavior was not set when a schema was pushed for index '"
					+ indexName + "': " + rootSchemaNode );
		}

		@Override
		public void prepareDocumentWorks(String indexName, List<StubDocumentWork> works) {
			throw new IllegalStateException( "The stub backend behavior was not set when works were prepared for index '"
					+ indexName + "': " + works );
		}

		@Override
		public CompletableFuture<?> executeDocumentWorks(String indexName, List<StubDocumentWork> works) {
			throw new IllegalStateException( "The stub backend behavior was not set when works were executed for index '"
					+ indexName + "': " + works );
		}

		@Override
		public CompletableFuture<?> prepareAndExecuteDocumentWork(String indexName, StubDocumentWork work) {
			throw new IllegalStateException( "The stub backend behavior was not set when work were prepared and executed for index '"
					+ indexName + "': " + work );
		}

		@Override
		public <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
				StubSearchProjectionContext projectionContext,
				LoadingContext<?, ?> loadingContext, StubSearchProjection<T> rootProjection) {
			throw new IllegalStateException( "The stub backend behavior was not set when a search work was executed for indexes "
					+ indexNames + ": " + work );
		}

		@Override
		public CompletableFuture<?> executeIndexScopeWork(Set<String> indexNames, StubIndexScopeWork work) {
			throw new IllegalStateException( "The stub backend behavior was not set during execution of an index-scope work for indexes "
					+ indexNames + ": " + work );
		}

		@Override
		public long executeCountWork(Set<String> indexNames) {
			throw new IllegalStateException( "The stub backend behavior was not set when a count work was executed for indexes "
					+ indexNames );
		}
	};

	private static final Map<String, StubBackendBehavior> BEHAVIORS = new HashMap<>();

	public static void set(String backendName, StubBackendBehavior behavior) {
		BEHAVIORS.put( backendName, behavior );
	}

	public static void unset(String backendName, StubBackendBehavior behavior) {
		BEHAVIORS.remove( backendName, behavior );
	}

	public static StubBackendBehavior get(String backendName) {
		return BEHAVIORS.getOrDefault( backendName, DEFAULT );
	}

	protected StubBackendBehavior() {
	}

	public abstract void onAddField(String indexName, String absoluteFieldPath);

	public abstract void pushSchema(String indexName, StubIndexSchemaNode rootSchemaNode);

	public abstract void prepareDocumentWorks(String indexName, List<StubDocumentWork> works);

	public abstract CompletableFuture<?> executeDocumentWorks(String indexName, List<StubDocumentWork> works);

	public abstract CompletableFuture<?> prepareAndExecuteDocumentWork(String indexName, StubDocumentWork work);

	public abstract <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext,
			LoadingContext<?, ?> loadingContext, StubSearchProjection<T> rootProjection);

	public abstract CompletableFuture<?> executeIndexScopeWork(Set<String> indexNames, StubIndexScopeWork work);

	public abstract long executeCountWork(Set<String> indexNames);
}
