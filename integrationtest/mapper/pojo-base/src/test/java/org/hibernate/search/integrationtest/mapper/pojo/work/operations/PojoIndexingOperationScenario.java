/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public abstract class PojoIndexingOperationScenario {

	public final BackendIndexingOperation expectedBackendOperation;

	public PojoIndexingOperationScenario(BackendIndexingOperation expectedBackendOperation) {
		this.expectedBackendOperation = expectedBackendOperation;
	}

	abstract boolean expectImplicitLoadingOnNullEntity();

	abstract boolean isEntityPresentOnLoading();

	abstract boolean expectSkipOnEntityAbsentAfterImplicitLoading();

	final <T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, T entity) {
		addTo( indexingPlan, providedId, null, entity );
	}

	abstract <T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
			T entity);

	final <T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId) {
		addWithoutInstanceTo( indexingPlan, entityClass, providedId, null );
	}

	abstract <T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
			DocumentRoutesDescriptor providedRoutes);

	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId, IndexedEntity entity) {
		return execute( indexer, providedId, null, entity );
	}

	abstract CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
			IndexedEntity entity);

	final CompletionStage<?> execute(SearchIndexer indexer, Object providedId) {
		return execute( indexer, providedId, (DocumentRoutesDescriptor) null );
	}

	abstract CompletionStage<?> execute(SearchIndexer indexer, Object providedId,
			DocumentRoutesDescriptor providedRoutes);
}
