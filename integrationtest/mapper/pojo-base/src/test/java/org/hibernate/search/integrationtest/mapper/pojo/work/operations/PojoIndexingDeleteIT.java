/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class PojoIndexingDeleteIT {

	private static final PojoIndexingOperationScenario SCENARIO = new PojoIndexingOperationScenario( BackendIndexingOperation.DELETE ) {
		@Override
		boolean expectImplicitLoadingOnNullEntity() {
			return false;
		}

		@Override
		boolean isEntityPresentOnLoading() {
			return false;
		}

		@Override
		boolean expectSkipOnEntityAbsentAfterImplicitLoading() {
			return false;
		}

		@Override
		<T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
				T entity) {
			indexingPlan.delete( providedId, providedRoutes, entity );
		}

		@Override
		<T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
				DocumentRoutesDescriptor providedRoutes) {
			indexingPlan.delete( entityClass, providedId, providedRoutes );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
				IndexedEntity entity) {
			return indexer.delete( providedId, providedRoutes, entity );
		}

		@Override
		CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
			return indexer.delete( IndexedEntity.class, providedId, providedRoutes );
		}
	};

	public static class IndexerBaseIT extends AbstractPojoIndexerOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	public static class IndexerNullEntityIT extends AbstractPojoIndexerDeleteNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	public static class IndexingPlanBaseIT extends AbstractPojoIndexingPlanOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	public static class IndexingPlanNullEntityIT extends AbstractPojoIndexingPlanOperationNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	public static class IndexingPlanContainedNullEntityIT extends AbstractPojoIndexingPlanOperationContainedNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

}
