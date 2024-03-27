/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class SpatialWithinCirclePredicateBaseIT {
	//CHECKSTYLE:ON

	private static final GeoPointFieldTypeDescriptor supportedFieldType;
	private static final List<
			FieldTypeDescriptor<GeoPoint, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedFieldTypes =
					new ArrayList<>();
	private static final List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedFieldTypes =
					new ArrayList<>();
	static {
		supportedFieldType = GeoPointFieldTypeDescriptor.INSTANCE;
		supportedFieldTypes.add( supportedFieldType );
		for ( FieldTypeDescriptor<?, ?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( !supportedFieldType.equals( fieldType ) ) {
				unsupportedFieldTypes.add( fieldType );
			}
		}
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldConfigured.index, MultiFieldConfigured.index,
						InObjectFieldConfigured.mainIndex, InObjectFieldConfigured.missingFieldIndex,
						ScoreConfigured.index,
						InvalidFieldConfigured.index, UnsupportedTypeConfigured.index,
						SearchableConfigured.searchableDefaultIndex, SearchableConfigured.searchableYesIndex,
						SearchableConfigured.searchableNoIndex,
						ArgumentCheckingConfigured.index,
						TypeCheckingNoConversionConfigured.index, TypeCheckingNoConversionConfigured.compatibleIndex,
						TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex,
						TypeCheckingNoConversionConfigured.missingFieldIndex,
						TypeCheckingNoConversionConfigured.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldConfigured.index.bulkIndexer();
		SingleFieldConfigured.dataSet.contribute( SingleFieldConfigured.index, singleFieldIndexer );

		final BulkIndexer multiFieldIndexer = MultiFieldConfigured.index.bulkIndexer();
		MultiFieldConfigured.dataSet.contribute( MultiFieldConfigured.index, multiFieldIndexer );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldConfigured.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldConfigured.missingFieldIndex.bulkIndexer();
		InObjectFieldConfigured.dataSet.contribute( InObjectFieldConfigured.mainIndex, inObjectFieldMainIndexer,
				InObjectFieldConfigured.missingFieldIndex, inObjectFieldMissingFieldIndexer );

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSet.contribute( ScoreConfigured.index, scoreIndexer );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionConfigured.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionConfigured.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer =
				TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingNoConversionConfigured.missingFieldIndex.bulkIndexer();
		TypeCheckingNoConversionConfigured.dataSet.contribute( TypeCheckingNoConversionConfigured.index,
				typeCheckingMainIndexer,
				TypeCheckingNoConversionConfigured.compatibleIndex, typeCheckingCompatibleIndexer,
				TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
				TypeCheckingNoConversionConfigured.missingFieldIndex, typeCheckingMissingFieldIndexer );

		singleFieldIndexer.join(
				multiFieldIndexer, inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer
		);
	}

	private static SpatialWithinCirclePredicateTestValues testValues() {
		return new SpatialWithinCirclePredicateTestValues();
	}

	@Nested
	class SingleFieldIT extends SingleFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SingleFieldConfigured extends AbstractPredicateSingleFieldIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.circle( SingleFieldConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							SingleFieldConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	@Nested
	class MultiFieldIT extends MultiFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class MultiFieldConfigured extends AbstractPredicateMultiFieldIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).field( otherFieldPath )
					.circle( MultiFieldConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							MultiFieldConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.circle( MultiFieldConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							MultiFieldConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).fields( fieldPaths )
					.circle( MultiFieldConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							MultiFieldConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	@Nested
	class InObjectFieldIT extends InObjectFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured
			extends AbstractPredicateFieldInObjectFieldIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet =
				new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( mainIndex, missingFieldIndex, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.circle( InObjectFieldConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							InObjectFieldConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}
	}

	@Nested
	class ScoreIT extends ScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured extends AbstractPredicateFieldScoreIT<SpatialWithinCirclePredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinCirclePredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.circle( ScoreConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							ScoreConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}
	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InvalidFieldConfigured extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldConfigured() {
			super( index );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-circle";
		}
	}

	@Nested
	class UnsupportedTypeIT extends UnsupportedTypeConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class UnsupportedTypeConfigured extends AbstractPredicateUnsupportedTypeIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, unsupportedFieldTypes ) )
						.name( "unsupportedType" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : unsupportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-circle";
		}
	}

	@Nested
	class SearchableIT extends SearchableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SearchableConfigured extends AbstractPredicateSearchableIT {
		private static final SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex =
				SimpleMappedIndex.of( root -> new SearchableDefaultIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableDefault" );
		private static final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex =
				SimpleMappedIndex.of( root -> new SearchableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableYes" );

		private static final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex =
				SimpleMappedIndex.of( root -> new SearchableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableNo" );

		public static List<? extends Arguments> params() {
			return Arrays.asList(
					Arguments.of( searchableDefaultIndex, searchableYesIndex, searchableNoIndex, supportedFieldType ) );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to circle()
					.circle( GeoPoint.of( 0, 0 ), 1 );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-circle";
		}
	}

	@Nested
	class ArgumentCheckingIT extends ArgumentCheckingConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ArgumentCheckingConfigured extends AbstractPredicateArgumentCheckingIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "argumentChecking" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, supportedFieldType ) );
		}

		@ParameterizedTest(name = "{1}")
		@MethodSource("params")
		void nullUnit(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?, ?> fieldType) {
			SearchPredicateFactory f = index.createScope().predicate();

			assertThatThrownBy(
					() -> f.spatial().within().field( fieldPath( index, fieldType ) ).circle( 0.0, 0.0, 10.0, null ) )
					.isInstanceOf( IllegalArgumentException.class )
					.hasMessageContainingAll( "must not be null" );
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath ).circle( null, 10.0 );
		}
	}

	@Nested
	class TypeCheckingNoConversionIT extends TypeCheckingNoConversionConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class TypeCheckingNoConversionConfigured
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
		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_missingField" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( root -> new IncompatibleIndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_incompatible" );

		public static List<? extends Arguments> params() {
			return Arrays.asList(
					Arguments.of( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex,
							dataSet )
			);
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.circle( TypeCheckingNoConversionConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							TypeCheckingNoConversionConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal, DataSet<?, SpatialWithinCirclePredicateTestValues> dataSet) {
			return f.spatial().within().field( field0Path ).field( field1Path )
					.circle( TypeCheckingNoConversionConfigured.dataSet.values.matchingCenter( matchingDocOrdinal ),
							TypeCheckingNoConversionConfigured.dataSet.values.matchingRadius( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-circle";
		}
	}
}
