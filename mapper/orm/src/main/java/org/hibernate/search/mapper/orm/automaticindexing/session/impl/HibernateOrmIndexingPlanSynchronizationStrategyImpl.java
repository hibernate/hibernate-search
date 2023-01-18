/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.session.impl;

import org.hibernate.search.mapper.orm.automaticindexing.session.HibernateOrmIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.AsyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.ReadSyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.SyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.WriteSyncIndexingPlanSynchronizationStrategy;

public final class HibernateOrmIndexingPlanSynchronizationStrategyImpl {

	public static final HibernateOrmIndexingPlanSynchronizationStrategy ASYNC = new HibernateOrmAsyncIndexingPlanSynchronizationStrategy();
	public static final HibernateOrmIndexingPlanSynchronizationStrategy WRITE_SYNC = new HibernateOrmWriteSyncIndexingPlanSynchronizationStrategy();
	public static final HibernateOrmIndexingPlanSynchronizationStrategy READ_SYNC = new HibernateOrmReadSyncIndexingPlanSynchronizationStrategy();
	public static final HibernateOrmIndexingPlanSynchronizationStrategy SYNC = new HibernateOrmSyncIndexingPlanSynchronizationStrategy();

	private HibernateOrmIndexingPlanSynchronizationStrategyImpl() {
	}

	private static final class HibernateOrmAsyncIndexingPlanSynchronizationStrategy extends
			AsyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements HibernateOrmIndexingPlanSynchronizationStrategy {
	}

	private static final class HibernateOrmWriteSyncIndexingPlanSynchronizationStrategy extends
			WriteSyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements HibernateOrmIndexingPlanSynchronizationStrategy {
	}

	private static final class HibernateOrmReadSyncIndexingPlanSynchronizationStrategy extends
			ReadSyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements HibernateOrmIndexingPlanSynchronizationStrategy {
	}

	private static final class HibernateOrmSyncIndexingPlanSynchronizationStrategy extends
			SyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements HibernateOrmIndexingPlanSynchronizationStrategy {
	}
}
