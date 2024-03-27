/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
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
class SpatialWithinPolygonPredicateBaseIT {
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

	private static SpatialWithinPolygonPredicateTestValues testValues() {
		return new SpatialWithinPolygonPredicateTestValues();
	}

	private static GeoPolygon unsusedPolygon() {
		return GeoPolygon.of( GeoPoint.of( 0.0, 0.0 ), GeoPoint.of( 1.0, 0.0 ),
				GeoPoint.of( 0.0, 1.0 ), GeoPoint.of( 0.0, 0.0 ) );
	}

	@Nested
	class SingleFieldIT extends SingleFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SingleFieldConfigured
			extends AbstractPredicateSingleFieldIT<SpatialWithinPolygonPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinPolygonPredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.polygon( SingleFieldConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class MultiFieldIT extends MultiFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class MultiFieldConfigured extends AbstractPredicateMultiFieldIT<SpatialWithinPolygonPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinPolygonPredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal, DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).field( otherFieldPath )
					.polygon( MultiFieldConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.polygon( MultiFieldConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).fields( fieldPaths )
					.polygon( MultiFieldConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class InObjectFieldIT extends InObjectFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured
			extends AbstractPredicateFieldInObjectFieldIT<SpatialWithinPolygonPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinPolygonPredicateTestValues> dataSet =
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
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.polygon( InObjectFieldConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	class ScoreIT extends ScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured extends AbstractPredicateFieldScoreIT<SpatialWithinPolygonPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinPolygonPredicateTestValues> dataSet = new DataSet<>( testValues() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		public static List<? extends Arguments> params() {
			return Arrays.asList( Arguments.of( index, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().fields( fieldPaths )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath ).boost( fieldBoost )
					.polygon( ScoreConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
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
					// We need this because the backend is not involved before the call to polygon()
					.polygon( unsusedPolygon() );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-polygon";
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
					// We need this because the backend is not involved before the call to polygon()
					.polygon( unsusedPolygon() );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-polygon";
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

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( searchableDefaultIndex, searchableYesIndex, searchableNoIndex, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return Arrays.asList(
					Arguments.of( searchableDefaultIndex, searchableYesIndex, searchableNoIndex, supportedFieldType ) );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.spatial().within().field( fieldPath )
					// We need this because the backend is not involved before the call to polygon()
					.polygon( unsusedPolygon() );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-polygon";
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

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.spatial().within().field( fieldPath ).polygon( null );
		}
	}

	@Nested
	class TypeCheckingNoConversionIT extends TypeCheckingNoConversionConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class TypeCheckingNoConversionConfigured
			extends AbstractPredicateTypeCheckingNoConversionIT<SpatialWithinPolygonPredicateTestValues> {
		private static final DataSet<GeoPoint, SpatialWithinPolygonPredicateTestValues> dataSet = new DataSet<>( testValues() );

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
							dataSet
					) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( fieldPath )
					.polygon( TypeCheckingNoConversionConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal, DataSet<?, SpatialWithinPolygonPredicateTestValues> dataSet) {
			return f.spatial().within().field( field0Path ).field( field1Path )
					.polygon( TypeCheckingNoConversionConfigured.dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:spatial:within-polygon";
		}
	}
}
