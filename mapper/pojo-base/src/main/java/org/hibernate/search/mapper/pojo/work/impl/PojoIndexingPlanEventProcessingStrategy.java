/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

/**
 * A strategy for processing indexing events sent by a {@link PojoIndexingPlanEventSendingStrategy}.
 */
public class PojoIndexingPlanEventProcessingStrategy extends PojoIndexingPlanLocalStrategy {
	public PojoIndexingPlanEventProcessingStrategy(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		super( commitStrategy, refreshStrategy );
	}

	@Override
	public boolean shouldResolveDirty() {
		// When processing indexing events sent from a queue,
		// reindexing resolution is performed before the events are sent to the queue,
		// so we don't do it again.
		return false;
	}
}
