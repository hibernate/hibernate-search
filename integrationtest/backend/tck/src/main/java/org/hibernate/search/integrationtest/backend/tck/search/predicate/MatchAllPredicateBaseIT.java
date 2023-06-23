/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NestedRunner.class)
public class MatchAllPredicateBaseIT {

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes(
						InObjectFieldIT.mainIndex, InObjectFieldIT.missingFieldIndex,
						ScoreIT.index
				)
				.setup();

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldIT.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldIT.missingFieldIndex.bulkIndexer();
		InObjectFieldIT.dataSet.contribute( inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSet.contribute( scoreIndexer );

		inObjectFieldMainIndexer.join( inObjectFieldMissingFieldIndexer, scoreIndexer );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	public static class InObjectFieldIT extends AbstractPredicateInObjectFieldIT {
		private static final DataSet dataSet = new DataSet();

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, FieldTypeDescriptor.getAll() ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, FieldTypeDescriptor.getAll() ) )
						.name( "nesting_missingField" );

		public InObjectFieldIT() {
			super( mainIndex, missingFieldIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
				int matchingDocOrdinal) {
			return f.matchAll().except( f.id().matchingAny( dataSet.docIdsExcept( matchingDocOrdinal ) ) );
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
	public static class ScoreIT extends AbstractPredicateScoreIT {
		private static final DataSet dataSet = new DataSet();

		private static final StubMappedIndex index = StubMappedIndex.withoutFields().name( "score" );

		public ScoreIT() {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.matchAll().except( f.id().matchingAny( dataSet.docIdsExcept( matchingDocOrdinal ) ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost) {
			return f.matchAll().except( f.id().matchingAny( dataSet.docIdsExcept( matchingDocOrdinal ) ) )
					.boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.matchAll().except( f.id().matchingAny( dataSet.docIdsExcept( matchingDocOrdinal ) ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost) {
			return f.matchAll().except( f.id().matchingAny( dataSet.docIdsExcept( matchingDocOrdinal ) ) )
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
