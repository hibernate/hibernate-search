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

class PojoIndexingAddIT {

	private static final PojoIndexingOperationScenario SCENARIO =
			new PojoIndexingOperationScenario( BackendIndexingOperation.ADD ) {
				@Override
				boolean expectImplicitLoadingOnNullEntity() {
					return true;
				}

				@Override
				boolean isEntityPresentOnLoading() {
					return true;
				}

				@Override
				boolean expectSkipOnEntityAbsentAfterImplicitLoading() {
					return true;
				}

				@Override
				<T> void addTo(SearchIndexingPlan indexingPlan, Object providedId, DocumentRoutesDescriptor providedRoutes,
						T entity) {
					indexingPlan.add( providedId, providedRoutes, entity );
				}

				@Override
				<T> void addWithoutInstanceTo(SearchIndexingPlan indexingPlan, Class<T> entityClass, Object providedId,
						DocumentRoutesDescriptor providedRoutes) {
					indexingPlan.add( entityClass, providedId, providedRoutes );
				}

				@Override
				CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes,
						IndexedEntity entity) {
					return indexer.add( providedId, providedRoutes, entity );
				}

				@Override
				CompletionStage<?> execute(SearchIndexer indexer, Object providedId, DocumentRoutesDescriptor providedRoutes) {
					return indexer.add( IndexedEntity.class, providedId, providedRoutes );
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
	class IndexerNullEntityIT extends AbstractPojoIndexerAddOrUpdateNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	class IndexerIndexingProcessorFailureIT extends AbstractPojoIndexingProcessorFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexer().add( entity );
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
	class IndexingPlanIndexingProcessorFailureIT extends AbstractPojoIndexingProcessorFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexingPlan().add( entity );
			session.close();
		}
	}

	@Nested
	class IndexingPlanReindexingResolutionFailureIT extends AbstractPojoReindexingResolutionFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexingPlan().add( entity );
			session.close();
		}
	}

}
