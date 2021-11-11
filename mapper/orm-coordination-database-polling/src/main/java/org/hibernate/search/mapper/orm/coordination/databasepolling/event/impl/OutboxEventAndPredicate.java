/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.util.HashMap;
import java.util.Map;

public class OutboxEventAndPredicate implements OutboxEventPredicate {

	public static OutboxEventAndPredicate of(OutboxEventPredicate left, OutboxEventPredicate right) {
		return new OutboxEventAndPredicate( left, right );
	}

	private final OutboxEventPredicate left;
	private final OutboxEventPredicate right;

	private OutboxEventAndPredicate(OutboxEventPredicate left, OutboxEventPredicate right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String queryPart(String eventAlias) {
		return "(" + left.queryPart( eventAlias ) + ") and (" + right.queryPart( eventAlias ) + ")";
	}

	@Override
	public Map<String, Object> params() {
		Map<String, Object> merged = new HashMap<>();
		// Assuming no conflicts...
		merged.putAll( left.params() );
		merged.putAll( right.params() );
		return merged;
	}
}
