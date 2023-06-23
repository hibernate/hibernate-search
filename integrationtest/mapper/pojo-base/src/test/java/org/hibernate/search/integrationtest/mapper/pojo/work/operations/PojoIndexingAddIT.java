/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.runner.RunWith;

@RunWith(NestedRunner.class)
public class PojoIndexingAddIT {

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
	public static class IndexerBaseIT extends AbstractPojoIndexerOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	public static class IndexerNullEntityIT extends AbstractPojoIndexerAddOrUpdateNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	public static class IndexerIndexingProcessorFailureIT extends AbstractPojoIndexingProcessorFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexer().add( entity );
		}
	}

	@Nested
	public static class IndexingPlanBaseIT extends AbstractPojoIndexingPlanOperationBaseIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	public static class IndexingPlanNullEntityIT extends AbstractPojoIndexingPlanOperationNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	public static class IndexingPlanContainedNullEntityIT extends AbstractPojoIndexingPlanOperationContainedNullEntityIT {
		@Override
		protected PojoIndexingOperationScenario scenario() {
			return SCENARIO;
		}
	}

	@Nested
	public static class IndexingPlanIndexingProcessorFailureIT extends AbstractPojoIndexingProcessorFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexingPlan().add( entity );
			session.close();
		}
	}

	@Nested
	public static class IndexingPlanReindexingResolutionFailureIT extends AbstractPojoReindexingResolutionFailureIT {
		@Override
		protected void process(SearchSession session, Object entity) {
			session.indexingPlan().add( entity );
			session.close();
		}
	}

}
