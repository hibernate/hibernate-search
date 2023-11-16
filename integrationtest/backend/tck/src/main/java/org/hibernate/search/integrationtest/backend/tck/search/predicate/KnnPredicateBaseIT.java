/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.opentest4j.TestAbortedException;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class KnnPredicateBaseIT {
	//CHECKSTYLE:ON

	private static final List<VectorFieldTypeDescriptor<?>> supportedFieldTypes = VectorFieldTypeDescriptor.getAllVector();

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( ScoreConfigured.index )
				.setup();

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSets.forEach( d -> d.contribute( ScoreConfigured.index, scoreIndexer ) );

		scoreIndexer.join();
	}

	@Nested
	class ScoreIT<F> extends ScoreConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured<F> extends AbstractPredicateFieldScoreIT<KnnPredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();

		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		void constantScore_fieldLevelBoost(SimpleMappedIndex<IndexBinding> index,
				DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			throw new TestAbortedException( "Field level score not supported" );
		}

		@Override
		protected KnnPredicateOptionsStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			F matchingArg = ( dataSet ).values.matchingArg( matchingDocOrdinal );
			if ( matchingArg instanceof byte[] ) {
				return f.knn( 1 ).field( fieldPath ).matching( ( (byte[]) matchingArg ) );
			}
			else {
				return f.knn( 1 ).field( fieldPath ).matching( ( (float[]) matchingArg ) );
			}
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPaths[0], matchingDocOrdinal, dataSet ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPaths[0], matchingDocOrdinal, dataSet ).boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPaths[0], matchingDocOrdinal, dataSet ).constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPath, matchingDocOrdinal, dataSet ).constantScore().boost( fieldBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal,
				DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPath, matchingDocOrdinal, dataSet ).constantScore().constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, KnnPredicateTestValues<F>> dataSet) {
			return predicate( f, fieldPath, matchingDocOrdinal, dataSet ).constantScore().boost( fieldBoost );
		}
	}

	private static <F> KnnPredicateTestValues<F> testValues(FieldTypeDescriptor<F, ?> fieldType) {
		return new KnnPredicateTestValues<>( fieldType );
	}
}
