/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.plan.synchronization.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.AsyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.ReadSyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.SyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.plan.synchronization.impl.WriteSyncIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.plan.synchronization.PojoStandaloneIndexingPlanSynchronizationStrategy;

public final class PojoStandaloneIndexingPlanSynchronizationStrategyImpl {

	public static final PojoStandaloneIndexingPlanSynchronizationStrategy ASYNC = new PojoStandaloneAsyncIndexingPlanSynchronizationStrategy();
	public static final PojoStandaloneIndexingPlanSynchronizationStrategy WRITE_SYNC = new PojoStandaloneWriteSyncIndexingPlanSynchronizationStrategy();
	public static final PojoStandaloneIndexingPlanSynchronizationStrategy READ_SYNC = new PojoStandaloneReadSyncIndexingPlanSynchronizationStrategy();
	public static final PojoStandaloneIndexingPlanSynchronizationStrategy SYNC = new PojoStandaloneSyncIndexingPlanSynchronizationStrategy();

	private PojoStandaloneIndexingPlanSynchronizationStrategyImpl() {
	}

	public static PojoStandaloneIndexingPlanSynchronizationStrategy from(
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy
	) {
		if ( DocumentCommitStrategy.NONE.equals( commitStrategy ) && DocumentRefreshStrategy.NONE.equals( refreshStrategy ) ) {
			return ASYNC;
		}
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) && DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			return SYNC;
		}
		if ( DocumentCommitStrategy.FORCE.equals( commitStrategy ) && DocumentRefreshStrategy.NONE.equals( refreshStrategy ) ) {
			return WRITE_SYNC;
		}
		if ( DocumentCommitStrategy.NONE.equals( commitStrategy ) && DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			return READ_SYNC;
		}
		throw new IllegalStateException( "This shouldn't happen." );
	}

	private static final class PojoStandaloneAsyncIndexingPlanSynchronizationStrategy extends
			AsyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements PojoStandaloneIndexingPlanSynchronizationStrategy {
	}

	private static final class PojoStandaloneWriteSyncIndexingPlanSynchronizationStrategy extends
			WriteSyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements PojoStandaloneIndexingPlanSynchronizationStrategy {
	}

	private static final class PojoStandaloneReadSyncIndexingPlanSynchronizationStrategy extends
			ReadSyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements PojoStandaloneIndexingPlanSynchronizationStrategy {
	}

	private static final class PojoStandaloneSyncIndexingPlanSynchronizationStrategy extends
			SyncIndexingPlanSynchronizationStrategy<EntityReference>
			implements PojoStandaloneIndexingPlanSynchronizationStrategy {
	}
}
