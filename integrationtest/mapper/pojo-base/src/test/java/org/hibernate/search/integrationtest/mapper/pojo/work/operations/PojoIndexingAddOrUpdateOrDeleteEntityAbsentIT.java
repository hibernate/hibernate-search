/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.junit.jupiter.api.Nested;

/**
 * Test behavior of "addOrUpdateOrDelete" when the entity is absent upon loading,
 * this resulting in a "delete".
 */
class PojoIndexingAddOrUpdateOrDeleteEntityAbsentIT {

	private static final PojoIndexingOperationScenario SCENARIO =
			new PojoIndexingAddOrUpdateOrDeleteScenario( BackendIndexingOperation.DELETE ) {
				@Override
				public boolean isEntityPresentOnLoading() {
					return false;
				}
			};

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

}
