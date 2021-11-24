/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendIndexingWorkExpectations;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public final class CoordinationStrategyExpectations {

	public static CoordinationStrategyExpectations defaults() {
		return new CoordinationStrategyExpectations( null, true, BackendIndexingWorkExpectations.sync() );
	}

	public static CoordinationStrategyExpectations outboxPolling() {
		return async( "outbox-polling", ".*Outbox event processor.*" );
	}

	private static CoordinationStrategyExpectations async(String strategyName, String threadNamePattern) {
		return new CoordinationStrategyExpectations( strategyName, false,
				BackendIndexingWorkExpectations.async( threadNamePattern, StubDocumentWork.Type.ADD_OR_UPDATE ) );
	}

	final String strategyName;
	final boolean sync;
	public final BackendIndexingWorkExpectations indexingWorkExpectations;

	private CoordinationStrategyExpectations(String strategyName, boolean sync,
			BackendIndexingWorkExpectations indexingWorkExpectations) {
		this.strategyName = strategyName;
		this.sync = sync;
		this.indexingWorkExpectations = indexingWorkExpectations;
	}

}
