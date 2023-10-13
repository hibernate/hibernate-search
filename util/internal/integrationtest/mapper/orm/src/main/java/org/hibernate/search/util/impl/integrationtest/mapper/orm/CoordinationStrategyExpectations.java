/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendIndexingWorkExpectations;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public final class CoordinationStrategyExpectations {

	public static CoordinationStrategyExpectations defaults() {
		return new CoordinationStrategyExpectations( null, true,
				BackendIndexingWorkExpectations.sync(), false );
	}

	public static CoordinationStrategyExpectations outboxPolling() {
		return new CoordinationStrategyExpectations( "outbox-polling", false,
				BackendIndexingWorkExpectations.async( ".*Outbox event processor.*",
						StubDocumentWork.Type.ADD_OR_UPDATE ),
				true );
	}

	public static CoordinationStrategyExpectations outboxPollingAndMassIndexing() {
		return new CoordinationStrategyExpectations( "outbox-polling", false,
				BackendIndexingWorkExpectations.async( ".*Outbox event processor.*|Hibernate Search - Mass indexing.*",
						// We expect the test to take into account that event processors always
						// issue "add-or-update" works, never "add" work; but the mass indexer does.
						StubDocumentWork.Type.ADD ),
				true );
	}

	final String strategyName;
	final boolean sync;
	public final BackendIndexingWorkExpectations indexingWorkExpectations;
	final boolean requiresTenantIds;

	private CoordinationStrategyExpectations(String strategyName, boolean sync,
			BackendIndexingWorkExpectations indexingWorkExpectations, boolean requiresTenantIds) {
		this.strategyName = strategyName;
		this.sync = sync;
		this.indexingWorkExpectations = indexingWorkExpectations;
		this.requiresTenantIds = requiresTenantIds;
	}

}
