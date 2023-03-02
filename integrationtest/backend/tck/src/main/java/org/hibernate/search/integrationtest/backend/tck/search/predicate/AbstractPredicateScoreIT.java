/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateScoreIT {
	private final StubMappedIndex index;
	private final AbstractPredicateDataSet dataSet;

	public AbstractPredicateScoreIT(StubMappedIndex index, AbstractPredicateDataSet dataSet) {
		this.index = index;
		this.dataSet = dataSet;
	}

	@Test
	public void predicateLevelBoost() {
		assertThatQuery( index.query()
				.where( f -> f.or(
						predicate( f, 0 ),
						predicateWithBoost( f, 1, 7f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithBoost( f, 0, 39f ),
						predicate( f, 1 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void constantScore() {
		assumeConstantScoreSupported();

		assertThatQuery( index.query()
				.where( f -> f.or(
						// Very low boost, so score << 1
						predicateWithBoost( f, 0, 0.01f ),
						// Constant score, so score = 1
						predicateWithConstantScore( f, 1 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						// Constant score, so score = 1
						predicateWithConstantScore( f, 0 ),
						// Very low boost, so score << 1
						predicateWithBoost( f, 1, 0.01f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	@Test
	public void constantScore_predicateLevelBoost() {
		assumeConstantScoreSupported();

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithConstantScoreAndBoost( f, 0, 7f ),
						predicateWithConstantScoreAndBoost( f, 1, 39f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 1 ), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.where( f -> f.or(
						predicateWithConstantScoreAndBoost( f, 0, 39f ),
						predicateWithConstantScoreAndBoost( f, 1, 7f ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsExactOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
			float boost);

	protected abstract PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f,
			int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
			int matchingDocOrdinal, float boost);

	protected void assumeConstantScoreSupported() {
		// By default we assume constant score IS supported.
	}

}
