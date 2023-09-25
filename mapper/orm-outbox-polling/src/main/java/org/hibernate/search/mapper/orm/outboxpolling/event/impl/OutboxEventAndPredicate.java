/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import org.hibernate.query.Query;

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
	public void setParams(Query<?> query) {
		// Assuming no conflicts...
		left.setParams( query );
		right.setParams( query );
	}
}
