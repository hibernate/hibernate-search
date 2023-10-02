/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class AndPredicateBaseIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						ScoreConfigured.index,
						AddScoreConfigured.index
				)
				.setup();

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSet.contribute( scoreIndexer );

		final BulkIndexer addScoreIndexer = AddScoreConfigured.index.bulkIndexer();
		AddScoreConfigured.dataSet.contribute( addScoreIndexer );

		scoreIndexer.join();
		addScoreIndexer.join();
	}

	@Nested
	class ScoreIT extends ScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured extends AbstractPredicateScoreIT {
		private static final DataSet dataSet = new DataSet();

		private static final StubMappedIndex index = StubMappedIndex.withoutFields().name( "score" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.and( f.id().matching( ScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.and( f.id().matching( ScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.and( f.id().matching( ScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.and( f.id().matching( ScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.constantScore()
					.boost( boost );
		}

		private static class DataSet extends AbstractPredicateDataSet {
			protected DataSet() {
				super( "singleRoutingKey" );
			}

			public void contribute(BulkIndexer scoreIndexer) {
				scoreIndexer.add( docId( 0 ), routingKey, document -> {} );
				scoreIndexer.add( docId( 1 ), routingKey, document -> {} );
				scoreIndexer.add( docId( 2 ), routingKey, document -> {} );
			}
		}
	}

	@Nested
	class AddScoreIT extends AddScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class AddScoreConfigured extends AbstractPredicateScoreIT {
		private static final DataSet dataSet = new DataSet();

		private static final StubMappedIndex index = StubMappedIndex.withoutFields().name( "addscore" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.and()
					.add( f.id().matching( AddScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.and( f.id().matching( AddScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.and()
					.add( f.id().matching( AddScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.and()
					.add( f.id().matching( AddScoreConfigured.dataSet.docId( matchingDocOrdinal ) ) )
					.constantScore()
					.boost( boost );
		}

		private static class DataSet extends AbstractPredicateDataSet {
			protected DataSet() {
				super( "singleRoutingKey" );
			}

			public void contribute(BulkIndexer scoreIndexer) {
				scoreIndexer.add( docId( 0 ), routingKey, document -> {} );
				scoreIndexer.add( docId( 1 ), routingKey, document -> {} );
				scoreIndexer.add( docId( 2 ), routingKey, document -> {} );
			}
		}
	}
}
