/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory<LuceneSearchAggregationCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchIndexScope scope;

	public LuceneSearchAggregationBuilderFactory(LuceneSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public <A> void contribute(LuceneSearchAggregationCollector collector,
			AggregationKey<A> key, SearchAggregation<A> aggregation) {
		if ( !(aggregation instanceof LuceneSearchAggregation) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherAggregations( aggregation );
		}

		LuceneSearchAggregation<A> casted = (LuceneSearchAggregation<A>) aggregation;
		if ( !scope.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
				aggregation, casted.getIndexNames(), scope.hibernateSearchIndexNames()
			);
		}

		collector.collectAggregation( key, casted );
	}

	@Override
	public <T> TermsAggregationBuilder<T> createTermsAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		return scope.field( absoluteFieldPath ).queryElement( AggregationTypeKeys.TERMS, scope )
				.type( expectedType, convert );
	}

	@Override
	public <T> RangeAggregationBuilder<T> createRangeAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		return scope.field( absoluteFieldPath ).queryElement( AggregationTypeKeys.RANGE, scope )
				.type( expectedType, convert );
	}
}
