/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.RangeQueryOptions;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;

/**
 * @author Guillaume Smet
 */
class RangePredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements RangePredicateBuilder<LuceneSearchPredicateCollector> {

	private final String absoluteFieldPath;

	private final LuceneFieldQueryFactory queryFactory;

	private Object lowerLimit;
	private Object upperLimit;

	private final RangeQueryOptions queryOptions = new RangeQueryOptions();

	public RangePredicateBuilderImpl(String absoluteFieldPath, LuceneFieldQueryFactory queryFactory) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.queryFactory = queryFactory;
	}

	@Override
	public void lowerLimit(Object value) {
		lowerLimit = value;
	}

	@Override
	public void excludeLowerLimit() {
		queryOptions.excludeLowerLimit();
	}

	@Override
	public void upperLimit(Object value) {
		upperLimit = value;
	}

	@Override
	public void excludeUpperLimit() {
		queryOptions.excludeUpperLimit();
	}

	@Override
	protected Query buildQuery() {
		return queryFactory.createRangeQuery( absoluteFieldPath, lowerLimit, upperLimit, queryOptions );
	}
}
