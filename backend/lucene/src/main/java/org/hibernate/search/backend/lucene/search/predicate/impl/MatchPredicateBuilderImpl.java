/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.MatchQueryOptions;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

/**
 * @author Guillaume Smet
 */
class MatchPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements MatchPredicateBuilder<LuceneSearchPredicateCollector> {

	private final String absoluteFieldPath;

	private final LuceneFieldQueryFactory queryBuilder;

	private final MatchQueryOptions queryOptions = new MatchQueryOptions();

	private Object value;

	public MatchPredicateBuilderImpl(String absoluteFieldPath, LuceneFieldQueryFactory queryBuilder) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public void value(Object value) {
		this.value = value;
	}

	@Override
	protected Query buildQuery() {
		// TODO GSM MatchQueryOptions will be populated once we have more options in the match predicate
		queryOptions.setOperator( Occur.SHOULD );

		return queryBuilder.createMatchQuery( absoluteFieldPath, value, queryOptions );
	}
}
