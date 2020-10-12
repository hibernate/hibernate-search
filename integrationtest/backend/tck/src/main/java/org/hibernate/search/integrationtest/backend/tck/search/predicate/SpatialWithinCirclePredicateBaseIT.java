/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class SpatialWithinCirclePredicateBaseIT {

	private static final GeoPointFieldTypeDescriptor supportedFieldType;
	private static final List<FieldTypeDescriptor<GeoPoint>> supportedFieldTypes = new ArrayList<>();
	private static final List<FieldTypeDescriptor<?>> unsupportedFieldTypes = new ArrayList<>();
	static {
		supportedFieldType = GeoPointFieldTypeDescriptor.INSTANCE;
		supportedFieldTypes.add( supportedFieldType );
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( !supportedFieldType.equals( fieldType ) ) {
				unsupportedFieldTypes.add( fieldType );
			}
		}
	}

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldIT.index, MultiFieldIT.index, NestingIT.index,
						ScoreIT.index,
						InvalidFieldIT.index, UnsupportedTypeIT.index,
						SearchableIT.searchableYesIndex, SearchableIT.searchableNoIndex,
						ArgumentCheckingIT.index,
						TypeCheckingNoConversionIT.index, TypeCheckingNoConversionIT.compatibleIndex,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex, TypeCheckingNoConversionIT.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldIT.index.bulkIndexer();
		SingleFieldIT.dataSet.contribute( SingleFieldIT.index, singleFieldIndexer );

		final BulkIndexer multiFieldIndexer = MultiFieldIT.index.bulkIndexer();
		MultiFieldIT.dataSet.contribute( MultiFieldIT.index, multiFieldIndexer );

		final BulkIndexer nestingIndexer = NestingIT.index.bulkIndexer();
		NestingIT.dataSet.contribute( NestingIT.index, nestingIndexer );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSet.contribute( ScoreIT.index, scoreIndexer );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionIT.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionIT.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer = TypeCheckingNoConversionIT.rawFieldCompatibleIndex.bulkIndexer();
		TypeCheckingNoConversionIT.dataSet.contribute( TypeCheckingNoConversionIT.index, typeCheckingMainIndexer,
				TypeCheckingNoConversionIT.compatibleIndex, typeCheckingCompatibleIndexer,
				TypeCheckingNoConversionIT.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer );

		singleFieldIndexer.join(
				multiFieldIndexer, nestingIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer, typeCheckingRawFieldCompatibleIndexer
		);
	}

	private static SpatialWithinCirclePredicateTestValues testValues() {
		return new SpatialWithinCirclePredicateTestValues();
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	public static class SingleFieldIT extends AbstractPredicateSingleFieldIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		public SingleFieldIT() {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	public static class MultiFieldIT extends AbstractPredicateMultiFieldIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		public MultiFieldIT() {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath ).field( otherFieldPath )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal) {
			return f.spatial().within().fields( fieldPaths )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath ).fields( fieldPaths )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	public static class NestingIT extends AbstractPredicateFieldNestingIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		public NestingIT() {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	public static class ScoreIT extends AbstractPredicateFieldScoreIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		public ScoreIT() {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal) {
			return f.spatial().within().fields( fieldPaths )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost) {
			return f.spatial().within().fields( fieldPaths )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost) {
			return f.spatial().within().fields( fieldPaths )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}
	}

	public static class InvalidFieldIT extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldIT() {
			super( index );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-circle";
		}
	}

	@RunWith(Parameterized.class)
	public static class UnsupportedTypeIT extends AbstractPredicateUnsupportedTypeIT {
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : unsupportedFieldTypes ) {
				parameters.add( new Object[] { fieldType } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, unsupportedFieldTypes ) )
						.name( "unsupportedType" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public UnsupportedTypeIT(FieldTypeDescriptor<?> fieldType) {
			super( index, fieldType );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-circle";
		}
	}

	public static class SearchableIT extends AbstractPredicateSearchableIT {
		private static final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex =
				SimpleMappedIndex.of( root -> new SearchableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableYes" );

		private static final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex =
				SimpleMappedIndex.of( root -> new SearchableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableNo" );

		public SearchableIT() {
			super( searchableYesIndex, searchableNoIndex, supportedFieldType );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-circle";
		}
	}

	public static class ArgumentCheckingIT extends AbstractPredicateArgumentCheckingIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "argumentChecking" );

		public ArgumentCheckingIT() {
			super( index, supportedFieldType );
		}

		@Test
		public void nullUnit() {
			SearchPredicateFactory f = index.createScope().predicate();

			assertThatThrownBy( () -> f.spatial().within().field( fieldPath() ).circle( 0.0, 0.0, 10.0, null ) )
					.isInstanceOf( IllegalArgumentException.class )
					.hasMessageContainingAll( "must not be null" );
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath ).circle( null, 10.0 );
		}
	}

	public static class TypeCheckingNoConversionIT
			extends AbstractPredicateTypeCheckingNoConversionIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_main" );
		private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
				SimpleMappedIndex.of( root -> new CompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_compatible" );
		private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
				SimpleMappedIndex.of( root -> new RawFieldCompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_rawFieldCompatible" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( root -> new IncompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_incompatible" );

		public TypeCheckingNoConversionIT() {
			super( index, compatibleIndex, rawFieldCompatibleIndex, incompatibleIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.spatial().within().field( fieldPath )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal) {
			return f.spatial().within().field( field0Path ).field( field1Path )
					.circle( dataSet.values.matchingCenter( matchingDocOrdinal ),
							dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-circle";
		}
	}
}
