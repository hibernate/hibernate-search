/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class SpatialWithinBoundingBoxPredicateBaseIT {
	//CHECKSTYLE:ON

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

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.createGlobal();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldIT.index, MultiFieldIT.index,
						InObjectFieldIT.mainIndex, InObjectFieldIT.missingFieldIndex,
						ScoreIT.index,
						InvalidFieldIT.index, UnsupportedTypeIT.index,
						SearchableIT.searchableYesIndex, SearchableIT.searchableNoIndex,
						ArgumentCheckingIT.index,
						TypeCheckingNoConversionIT.index, TypeCheckingNoConversionIT.compatibleIndex,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex, TypeCheckingNoConversionIT.missingFieldIndex,
						TypeCheckingNoConversionIT.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldIT.index.bulkIndexer();
		SingleFieldIT.dataSet.contribute( SingleFieldIT.index, singleFieldIndexer );

		final BulkIndexer multiFieldIndexer = MultiFieldIT.index.bulkIndexer();
		MultiFieldIT.dataSet.contribute( MultiFieldIT.index, multiFieldIndexer );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldIT.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldIT.missingFieldIndex.bulkIndexer();
		InObjectFieldIT.dataSet.contribute( InObjectFieldIT.mainIndex, inObjectFieldMainIndexer,
				InObjectFieldIT.missingFieldIndex, inObjectFieldMissingFieldIndexer );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSet.contribute( ScoreIT.index, scoreIndexer );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionIT.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionIT.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer =
				TypeCheckingNoConversionIT.rawFieldCompatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingNoConversionIT.missingFieldIndex.bulkIndexer();
		TypeCheckingNoConversionIT.dataSet.contribute( TypeCheckingNoConversionIT.index, typeCheckingMainIndexer,
				TypeCheckingNoConversionIT.compatibleIndex, typeCheckingCompatibleIndexer,
				TypeCheckingNoConversionIT.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
				TypeCheckingNoConversionIT.missingFieldIndex, typeCheckingMissingFieldIndexer );

		singleFieldIndexer.join(
				multiFieldIndexer, inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer
		);
	}

	private static SpatialWithinBoundingBoxPredicateTestValues testValues() {
		return new SpatialWithinBoundingBoxPredicateTestValues();
	}

	@Nested
	class SingleFieldIT extends AbstractPredicateSingleFieldIT<SpatialWithinBoundingBoxPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinBoundingBoxPredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.boundingBox( SingleFieldIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class MultiFieldIT extends AbstractPredicateMultiFieldIT<SpatialWithinBoundingBoxPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinBoundingBoxPredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).field( otherFieldPath )
					.boundingBox( MultiFieldIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.boundingBox( MultiFieldIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).fields( fieldPaths )
					.boundingBox( MultiFieldIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class InObjectFieldIT
			extends AbstractPredicateFieldInObjectFieldIT<SpatialWithinBoundingBoxPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinBoundingBoxPredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList(
					Arguments.of( mainIndex, missingFieldIndex, dataSet )
			);
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.boundingBox( InObjectFieldIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class ScoreIT extends AbstractPredicateFieldScoreIT<SpatialWithinBoundingBoxPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinBoundingBoxPredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.boundingBox( ScoreIT.dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
		}
	}

	@Nested
	class InvalidFieldIT extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldIT() {
			super( index );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to boundingBox()
					.boundingBox( 0.0, 0.0, 0.1, 0.1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-bounding-box";
		}
	}

	@Nested
	class UnsupportedTypeIT extends AbstractPredicateUnsupportedTypeIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, unsupportedFieldTypes ) )
						.name( "unsupportedType" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : unsupportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to boundingBox()
					.boundingBox( 0.0, 0.0, 0.1, 0.1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-bounding-box";
		}
	}

	@Nested
	class SearchableIT extends AbstractPredicateSearchableIT {
		private static final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex =
				SimpleMappedIndex.of( root -> new SearchableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableYes" );

		private static final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex =
				SimpleMappedIndex.of( root -> new SearchableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableNo" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( searchableYesIndex, searchableNoIndex, supportedFieldType ) );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to boundingBox()
					.boundingBox( 0.0, 0.0, 0.1, 0.1 );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-bounding-box";
		}
	}

	@Nested
	class ArgumentCheckingIT extends AbstractPredicateArgumentCheckingIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "argumentChecking" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, supportedFieldType ) );
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath ).boundingBox( null );
		}
	}

	@Nested
	class TypeCheckingNoConversionIT
			extends AbstractPredicateTypeCheckingNoConversionIT<SpatialWithinBoundingBoxPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinBoundingBoxPredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_main" );
		private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
				SimpleMappedIndex.of( root -> new CompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_compatible" );
		private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
				SimpleMappedIndex.of( root -> new RawFieldCompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_rawFieldCompatible" );
		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_missingField" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( root -> new IncompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_incompatible" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex,
					incompatibleIndex, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.boundingBox( TypeCheckingNoConversionIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal, DataSet<?, SpatialWithinBoundingBoxPredicateTestValues> dataSet) {
			return f.spatial().within().field( field0Path ).field( field1Path )
					.boundingBox( TypeCheckingNoConversionIT.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:spatial:within-bounding-box";
		}
	}
}
