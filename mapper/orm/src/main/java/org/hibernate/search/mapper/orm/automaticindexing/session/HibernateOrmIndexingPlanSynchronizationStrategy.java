/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.session;

import org.hibernate.search.mapper.orm.automaticindexing.session.impl.HibernateOrmIndexingPlanSynchronizationStrategyImpl;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * @see SearchSession#indexingPlanSynchronizationStrategy(HibernateOrmIndexingPlanSynchronizationStrategy)
 */
public interface HibernateOrmIndexingPlanSynchronizationStrategy extends IndexingPlanSynchronizationStrategy<EntityReference> {

	/**
	 * @return A strategy that only waits for index changes to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static HibernateOrmIndexingPlanSynchronizationStrategy async() {
		return HibernateOrmIndexingPlanSynchronizationStrategyImpl.ASYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit, and waits for the commit to complete.
	 * See the reference documentation for details.
	 */
	static HibernateOrmIndexingPlanSynchronizationStrategy writeSync() {
		return HibernateOrmIndexingPlanSynchronizationStrategyImpl.WRITE_SYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a refresh, and waits for the refresh to complete.
	 * See the reference documentation for details.
	 */
	static HibernateOrmIndexingPlanSynchronizationStrategy readSync() {
		return HibernateOrmIndexingPlanSynchronizationStrategyImpl.READ_SYNC;
	}

	/**
	 * @return A strategy that waits for index changes to be queued and applied, forces a commit and a refresh,
	 * and waits for the commit and refresh to complete.
	 * See the reference documentation for details.
	 */
	static HibernateOrmIndexingPlanSynchronizationStrategy sync() {
		return HibernateOrmIndexingPlanSynchronizationStrategyImpl.SYNC;
	}

}
