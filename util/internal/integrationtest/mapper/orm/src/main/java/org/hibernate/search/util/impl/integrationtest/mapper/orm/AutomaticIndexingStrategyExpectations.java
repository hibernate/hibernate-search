/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendWorkThreadingExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.automaticindexing.LocalHeapQueueAutomaticIndexingStrategy;

public final class AutomaticIndexingStrategyExpectations {

	public static AutomaticIndexingStrategyExpectations defaults() {
		return new AutomaticIndexingStrategyExpectations( null, true, BackendWorkThreadingExpectations.sync() );
	}

	public static AutomaticIndexingStrategyExpectations localHeapQueue() {
		return async( LocalHeapQueueAutomaticIndexingStrategy.class.getName(), ".*Local heap queue.*" );
	}

	public static AutomaticIndexingStrategyExpectations outboxPolling() {
		return async( AutomaticIndexingStrategyNames.OUTBOX_POLLING, ".*Outbox table.*" );
	}

	private static AutomaticIndexingStrategyExpectations async(String strategyName, String threadNamePattern) {
		return new AutomaticIndexingStrategyExpectations( strategyName, false,
				BackendWorkThreadingExpectations.async( threadNamePattern ) );
	}

	final String strategyName;
	final boolean sync;
	final BackendWorkThreadingExpectations indexingWorkThreadingExpectations;

	private AutomaticIndexingStrategyExpectations(String strategyName, boolean sync,
			BackendWorkThreadingExpectations indexingWorkThreadingExpectations) {
		this.strategyName = strategyName;
		this.sync = sync;
		this.indexingWorkThreadingExpectations = indexingWorkThreadingExpectations;
	}

}
