/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.plan.synchronization;

import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.plan.synchronization.impl.PojoStandaloneIndexingPlanSynchronizationStrategyImpl;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Provides access to preconfigured synchronization strategies.
 *
 * {@code SearchSession#automaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy)}
 */
@Incubating
public interface PojoStandaloneIndexingPlanSynchronizationStrategy extends IndexingPlanSynchronizationStrategy<EntityReference> {

	/**
	 * @return A strategy that only waits for index changes to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static PojoStandaloneIndexingPlanSynchronizationStrategy async() {
		return PojoStandaloneIndexingPlanSynchronizationStrategyImpl.ASYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit, and waits for the commit to complete.
	 * See the reference documentation for details.
	 */
	static PojoStandaloneIndexingPlanSynchronizationStrategy writeSync() {
		return PojoStandaloneIndexingPlanSynchronizationStrategyImpl.WRITE_SYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a refresh, and waits for the refresh to complete.
	 * See the reference documentation for details.
	 */
	static PojoStandaloneIndexingPlanSynchronizationStrategy readSync() {
		return PojoStandaloneIndexingPlanSynchronizationStrategyImpl.READ_SYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit and a refresh,
	 * and waits for the commit and refresh to complete.
	 * See the reference documentation for details.
	 */
	static PojoStandaloneIndexingPlanSynchronizationStrategy sync() {
		return PojoStandaloneIndexingPlanSynchronizationStrategyImpl.SYNC;
	}

}
