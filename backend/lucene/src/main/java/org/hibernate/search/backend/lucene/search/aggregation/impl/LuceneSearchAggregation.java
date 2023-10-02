/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public interface LuceneSearchAggregation<A> extends SearchAggregation<A> {
	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * Request the collection of per-document data that will be used in
	 * {@link Extractor#extract(AggregationExtractContext)},
	 * making sure that the requirements for this projection are met.
	 *
	 * @param context A context that will share its state with the context passed to
	 * {@link Extractor#extract(AggregationExtractContext)}.
	 */
	Extractor<A> request(AggregationRequestContext context);


	Set<String> indexNames();

	interface Extractor<T> {
		/**
		 * Extract the result of the aggregation from the response.
		 *
		 * @param context The extract context, to extract information from the response's JSON body
		 * or retrieve information that was stored earlier in {@link #request(AggregationRequestContext)}.
		 * @return The aggregation result extracted from the response.
		 */
		T extract(AggregationExtractContext context) throws IOException;
	}

	static <A> LuceneSearchAggregation<A> from(LuceneSearchIndexScope<?> scope,
			SearchAggregation<A> aggregation) {
		if ( !( aggregation instanceof LuceneSearchAggregation ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherAggregations( aggregation );
		}

		LuceneSearchAggregation<A> casted = (LuceneSearchAggregation<A>) aggregation;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
					aggregation, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() )
			);
		}
		return casted;
	}


}
