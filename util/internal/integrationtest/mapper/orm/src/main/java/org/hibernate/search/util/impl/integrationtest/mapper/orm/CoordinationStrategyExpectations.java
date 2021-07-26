/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.mapper.orm.coordination.CoordinationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendWorkThreadingExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.coordination.localheap.LocalHeapQueueCooordinationStrategy;

public final class CoordinationStrategyExpectations {

	public static CoordinationStrategyExpectations defaults() {
		return new CoordinationStrategyExpectations( null, true, BackendWorkThreadingExpectations.sync() );
	}

	public static CoordinationStrategyExpectations localHeapQueue() {
		return async( LocalHeapQueueCooordinationStrategy.class.getName(), ".*Local heap queue.*" );
	}

	public static CoordinationStrategyExpectations outboxPolling() {
		return async( CoordinationStrategyNames.DATABASE_POLLING, ".*Outbox event processor.*" );
	}

	private static CoordinationStrategyExpectations async(String strategyName, String threadNamePattern) {
		return new CoordinationStrategyExpectations( strategyName, false,
				BackendWorkThreadingExpectations.async( threadNamePattern ) );
	}

	final String strategyName;
	final boolean sync;
	final BackendWorkThreadingExpectations indexingWorkThreadingExpectations;

	private CoordinationStrategyExpectations(String strategyName, boolean sync,
			BackendWorkThreadingExpectations indexingWorkThreadingExpectations) {
		this.strategyName = strategyName;
		this.sync = sync;
		this.indexingWorkThreadingExpectations = indexingWorkThreadingExpectations;
	}

}
