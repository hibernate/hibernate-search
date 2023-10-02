/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class MatchAllPredicateBaseIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						InObjectFieldConfigured.mainIndex, InObjectFieldConfigured.missingFieldIndex,
						ScoreConfigured.index
				)
				.setup();

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldConfigured.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldConfigured.missingFieldIndex.bulkIndexer();
		InObjectFieldConfigured.dataSet.contribute( inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer );

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSet.contribute( scoreIndexer );

		inObjectFieldMainIndexer.join( inObjectFieldMissingFieldIndexer, scoreIndexer );
	}

	@Nested
	class InObjectFieldIT extends InObjectFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured extends AbstractPredicateInObjectFieldIT {
		private static final DataSet dataSet = new DataSet();

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, FieldTypeDescriptor.getAll() ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, FieldTypeDescriptor.getAll() ) )
						.name( "nesting_missingField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( mainIndex, missingFieldIndex, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
				int matchingDocOrdinal, AbstractPredicateDataSet dataSet) {
			return f.matchAll()
					.except( f.id().matchingAny( InObjectFieldConfigured.dataSet.docIdsExcept( matchingDocOrdinal ) ) );
		}

		private static class DataSet extends AbstractPredicateDataSet {
			protected DataSet() {
				super( "singleRoutingKey" );
			}

			public void contribute(BulkIndexer mainIndexer, BulkIndexer missingFieldIndexer) {
				mainIndexer.add( docId( 0 ), routingKey, document -> mainIndex.binding()
						.initDocument( document, AnalyzedStringFieldTypeDescriptor.INSTANCE, "irrelevant" ) );
				mainIndexer.add( docId( 1 ), routingKey, document -> mainIndex.binding()
						.initDocument( document, AnalyzedStringFieldTypeDescriptor.INSTANCE, "irrelevant" ) );
				missingFieldIndexer.add( docId( MISSING_FIELD_INDEX_DOC_ORDINAL ), routingKey,
						document -> missingFieldIndex.binding().initDocument() );
			}

			public List<String> docIdsExcept(int docOrdinal) {
				return IntStream.concat(
						IntStream.range( 0, 3 ).filter( i -> i != docOrdinal ),
						IntStream.of( MISSING_FIELD_INDEX_DOC_ORDINAL )
				)
						.mapToObj( this::docId ).collect( Collectors.toList() );
			}
		}
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
			return f.matchAll().except( f.id().matchingAny( ScoreConfigured.dataSet.docIdsExcept( matchingDocOrdinal ) ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.matchAll().except( f.id().matchingAny( ScoreConfigured.dataSet.docIdsExcept( matchingDocOrdinal ) ) )
					.boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.matchAll().except( f.id().matchingAny( ScoreConfigured.dataSet.docIdsExcept( matchingDocOrdinal ) ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.matchAll().except( f.id().matchingAny( ScoreConfigured.dataSet.docIdsExcept( matchingDocOrdinal ) ) )
					.constantScore().boost( boost );
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

			public List<String> docIdsExcept(int docOrdinal) {
				return IntStream.range( 0, 3 ).filter( i -> i != docOrdinal )
						.mapToObj( this::docId ).collect( Collectors.toList() );
			}
		}
	}
}
