/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.CommonQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
abstract class AbstractBaseQueryStringPredicateBaseIT<P extends CommonQueryStringPredicateFieldStep<?>> {
	//CHECKSTYLE:ON

	private static final List<
			FieldTypeDescriptor<String, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedFieldTypes =
					new ArrayList<>();
	private static final List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedFieldTypes =
					new ArrayList<>();
	static {
		for ( FieldTypeDescriptor<?, ?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( String.class.equals( fieldType.getJavaType() ) ) {
				@SuppressWarnings("unchecked")
				FieldTypeDescriptor<String, ?> casted = (FieldTypeDescriptor<String, ?>) fieldType;
				supportedFieldTypes.add( casted );
			}
			else {
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
						AnalysisConfigured.index, AnalysisConfigured.compatibleIndex, AnalysisConfigured.incompatibleIndex,
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
		SingleFieldConfigured.dataSets.forEach( d -> d.contribute( SingleFieldConfigured.index, singleFieldIndexer ) );

		final BulkIndexer multiFieldIndexer = MultiFieldConfigured.index.bulkIndexer();
		MultiFieldConfigured.dataSets.forEach( d -> d.contribute( MultiFieldConfigured.index, multiFieldIndexer ) );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldConfigured.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldConfigured.missingFieldIndex.bulkIndexer();
		InObjectFieldConfigured.dataSets
				.forEach( d -> d.contribute( InObjectFieldConfigured.mainIndex, inObjectFieldMainIndexer,
						InObjectFieldConfigured.missingFieldIndex, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer analysisMainIndexIndexer = AnalysisConfigured.index.bulkIndexer();
		final BulkIndexer analysisCompatibleIndexIndexer = AnalysisConfigured.compatibleIndex.bulkIndexer();
		final BulkIndexer analysisIncompatibleIndexIndexer = AnalysisConfigured.incompatibleIndex.bulkIndexer();
		AnalysisConfigured.dataSet.contribute( AnalysisConfigured.index, analysisMainIndexIndexer,
				AnalysisConfigured.compatibleIndex, analysisCompatibleIndexIndexer,
				AnalysisConfigured.incompatibleIndex, analysisIncompatibleIndexIndexer );

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSets.forEach( d -> d.contribute( ScoreConfigured.index, scoreIndexer ) );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionConfigured.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionConfigured.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer =
				TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingNoConversionConfigured.missingFieldIndex.bulkIndexer();
		TypeCheckingNoConversionConfigured.dataSets
				.forEach( d -> d.contribute( TypeCheckingNoConversionConfigured.index, typeCheckingMainIndexer,
						TypeCheckingNoConversionConfigured.compatibleIndex, typeCheckingCompatibleIndexer,
						TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
						TypeCheckingNoConversionConfigured.missingFieldIndex, typeCheckingMissingFieldIndexer ) );

		singleFieldIndexer.join(
				multiFieldIndexer, inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer,
				analysisMainIndexIndexer, analysisCompatibleIndexIndexer, analysisIncompatibleIndexIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer
		);
	}

	private static CommonQueryStringPredicateTestValues testValues(FieldTypeDescriptor<String, ?> fieldType) {
		return new CommonQueryStringPredicateTestValues( fieldType );
	}

	@Nested
	class SingleFieldIT extends SingleFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class SingleFieldConfigured extends AbstractPredicateSingleFieldIT<CommonQueryStringPredicateTestValues> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		private static final List<DataSet<String, CommonQueryStringPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String, ?> fieldType : supportedFieldTypes ) {
				DataSet<String, CommonQueryStringPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class MultiFieldIT extends MultiFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class MultiFieldConfigured extends AbstractPredicateMultiFieldIT<CommonQueryStringPredicateTestValues> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		private static final List<DataSet<String, CommonQueryStringPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String, ?> fieldType : supportedFieldTypes ) {
				DataSet<String, CommonQueryStringPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).field( otherFieldPath )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).fields( fieldPaths ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).fields( fieldPaths )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class InObjectFieldIT extends InObjectFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class InObjectFieldConfigured
			extends AbstractPredicateFieldInObjectFieldIT<CommonQueryStringPredicateTestValues> {
		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String, ?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( mainIndex, missingFieldIndex, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class AnalysisIT extends AnalysisConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class AnalysisConfigured extends AbstractPredicateConfigurableAnalysisIT {
		private static final DataSet dataSet = new DataSet();

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "analysis_main" );
		private static final SimpleMappedIndex<IndexBinding> compatibleIndex = SimpleMappedIndex.of( IndexBinding::new )
				.name( "analysis_compatible" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "analysis_incompatible" );

		public AnalysisConfigured() {
			super( index, compatibleIndex, incompatibleIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, String matchingParam) {
			return predicate( f ).field( fieldPath ).matching( matchingParam );
		}

		@Override
		protected PredicateFinalStep predicateWithAnalyzerOverride(SearchPredicateFactory f, String fieldPath,
				String matchingParam, String analyzerName) {
			return predicate( f ).field( fieldPath ).matching( matchingParam ).analyzer( analyzerName );
		}

		@Override
		protected PredicateFinalStep predicateWithSkipAnalysis(SearchPredicateFactory f, String fieldPath,
				String matchingParam) {
			return predicate( f ).field( fieldPath ).matching( matchingParam ).skipAnalysis();
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class ScoreIT extends ScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class ScoreConfigured extends AbstractPredicateFieldScoreIT<CommonQueryStringPredicateTestValues> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		private static final List<DataSet<String, CommonQueryStringPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String, ?> fieldType : supportedFieldTypes ) {
				DataSet<String, CommonQueryStringPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		public void constantScore_fieldLevelBoost(SimpleMappedIndex<IndexBinding> index,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			throw new org.opentest4j.TestAbortedException(
					"The simpleQueryString predicate currently does not fail when using constantScore() + a field-level boost" );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).fields( fieldPaths )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).fields( fieldPaths )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).fields( fieldPaths )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}

		@Override
		protected String predicateTrait() {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicateTrait();
		}

	}

	abstract static class InvalidFieldConfigured extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		protected InvalidFieldConfigured() {
			super( index );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			predicate( f ).field( fieldPath );
		}

		protected abstract String predicateTrait();

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class UnsupportedTypeIT extends UnsupportedTypeConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}

		@Override
		protected String predicateTrait() {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicateTrait();
		}
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
			predicate( f ).field( fieldPath );
		}

		protected abstract String predicateTrait();

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class SearchableIT extends SearchableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...

		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}

		@Override
		protected String predicateTrait() {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicateTrait();
		}
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
			return parameters;
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			predicate( f ).field( fieldPath );
		}

		protected abstract String predicateTrait();

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class ArgumentCheckingIT extends ArgumentCheckingConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}
	}

	abstract static class ArgumentCheckingConfigured extends AbstractPredicateArgumentCheckingIT {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "argumentChecking" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			predicate( f ).field( fieldPath ).matching( null );
		}

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	@Nested
	class TypeCheckingNoConversionIT extends TypeCheckingNoConversionConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
		@Override
		CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicate( f );
		}

		@Override
		protected String predicateTrait() {
			return AbstractBaseQueryStringPredicateBaseIT.this.predicateTrait();
		}
	}

	abstract static class TypeCheckingNoConversionConfigured
			extends AbstractPredicateTypeCheckingNoConversionIT<CommonQueryStringPredicateTestValues> {
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

		private static final List<DataSet<String, CommonQueryStringPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String, ?> fieldType : supportedFieldTypes ) {
				DataSet<String, CommonQueryStringPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex,
						incompatibleIndex, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal, DataSet<?, CommonQueryStringPredicateTestValues> dataSet) {
			return predicate( f ).field( field0Path ).field( field1Path )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		protected abstract String predicateTrait();

		abstract CommonQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f);
	}

	abstract P predicate(SearchPredicateFactory f);

	protected abstract String predicateTrait();
}
