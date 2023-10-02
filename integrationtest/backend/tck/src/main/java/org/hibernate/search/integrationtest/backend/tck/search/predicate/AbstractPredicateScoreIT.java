/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateScoreIT {

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void predicateLevelBoost(StubMappedIndex index, AbstractPredicateDataSet dataSet) {
		assertThatQuery( index.query()
				.where( f -> f.or(
						predicate( f, 0, dataSet, index ),
						predicateWithBoost( f, 1, 7f, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithBoost( f, 0, 39f, dataSet, index ),
						predicate( f, 1, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void constantScore(StubMappedIndex index, AbstractPredicateDataSet dataSet) {
		assumeConstantScoreSupported();

		assertThatQuery( index.query()
				.where( f -> f.or(
						// Very low boost, so score << 1
						predicateWithBoost( f, 0, 0.01f, dataSet, index ),
						// Constant score, so score = 1
						predicateWithConstantScore( f, 1, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						// Constant score, so score = 1
						predicateWithConstantScore( f, 0, dataSet, index ),
						// Very low boost, so score << 1
						predicateWithBoost( f, 1, 0.01f, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void constantScore_predicateLevelBoost(StubMappedIndex index, AbstractPredicateDataSet dataSet) {
		assumeConstantScoreSupported();

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithConstantScoreAndBoost( f, 0, 7f, dataSet, index ),
						predicateWithConstantScoreAndBoost( f, 1, 39f, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithConstantScoreAndBoost( f, 0, 39f, dataSet, index ),
						predicateWithConstantScoreAndBoost( f, 1, 7f, dataSet, index ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
			AbstractPredicateDataSet dataSet, StubMappedIndex index);

	protected abstract PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
			float boost, AbstractPredicateDataSet dataSet,
			StubMappedIndex index);

	protected abstract PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f,
			int matchingDocOrdinal, AbstractPredicateDataSet dataSet,
			StubMappedIndex index);

	protected abstract PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
			int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
			StubMappedIndex index);

	protected void assumeConstantScoreSupported() {
		// By default we assume constant score IS supported.
	}

}
