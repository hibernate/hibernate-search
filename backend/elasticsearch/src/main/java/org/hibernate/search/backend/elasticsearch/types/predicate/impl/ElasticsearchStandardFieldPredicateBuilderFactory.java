/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;

public class ElasticsearchStandardFieldPredicateBuilderFactory<F>
		extends AbstractElasticsearchFieldPredicateBuilderFactory<F> {


	public ElasticsearchStandardFieldPredicateBuilderFactory(boolean searchable, ElasticsearchFieldCodec<F> codec) {
		super( searchable, codec );
	}

	@Override
	public MatchPredicateBuilder createMatchPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field) {
		checkSearchable( field );
		return new ElasticsearchStandardMatchPredicate.Builder<>( searchContext, field, codec );
	}

	@Override
	public RangePredicateBuilder createRangePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field) {
		checkSearchable( field );
		return new ElasticsearchRangePredicate.Builder<>( searchContext, field, codec );
	}

}
