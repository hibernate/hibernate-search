/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.RangeMatchingContext;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class ConnectedRangeContext implements RangeContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;

	public ConnectedRangeContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
	}

	@Override
	public RangeMatchingContext onField(String fieldName) {
		return new ConnectedRangeMatchingContext( fieldName, queryCustomizer, queryContext );
	}

	@Override
	public RangeContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public RangeContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public RangeContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
