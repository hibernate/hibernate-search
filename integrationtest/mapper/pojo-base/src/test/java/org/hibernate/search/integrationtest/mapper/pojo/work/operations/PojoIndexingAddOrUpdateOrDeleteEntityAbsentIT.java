/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
