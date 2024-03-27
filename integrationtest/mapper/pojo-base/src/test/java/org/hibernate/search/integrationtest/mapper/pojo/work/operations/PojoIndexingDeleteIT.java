/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;

import org.junit.jupiter.api.Nested;

class PojoIndexingDeleteIT {

	private static final PojoIndexingOperationScenario SCENARIO =
			new PojoIndexingOperationScenario( BackendIndexingOperation.DELETE ) {
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
					indexingPlan.purge( entityClass, providedId, providedRoutes );
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

	@Nested
	class IndexerBaseIT extends AbstractPojoIndexerOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexerNullEntityIT extends AbstractPojoIndexerDeleteNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexingPlanBaseIT extends AbstractPojoIndexingPlanOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexingPlanNullEntityIT extends AbstractPojoIndexingPlanOperationNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexingPlanContainedNullEntityIT extends AbstractPojoIndexingPlanOperationContainedNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexingPlanReindexingResolutionFailureIT extends AbstractPojoReindexingResolutionFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexingPlan().delete( entity );
			session.close();
		}
	}

}
