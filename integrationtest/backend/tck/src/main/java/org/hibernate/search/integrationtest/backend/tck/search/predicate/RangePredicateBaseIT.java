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

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.Range;
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
class RangePredicateBaseIT {
	//CHECKSTYLE:ON

	static final List<FieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	static final List<FieldTypeDescriptor<?>> unsupportedFieldTypes = new ArrayList<>();
	static {
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
				unsupportedFieldTypes.add( fieldType );
			}
			else {
				supportedFieldTypes.add( fieldType );
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
						SearchableConfigured.searchableYesIndex, SearchableConfigured.searchableNoIndex,
						ArgumentCheckingConfigured.index,
						TypeCheckingAndConversionConfigured.index, TypeCheckingAndConversionConfigured.compatibleIndex,
						TypeCheckingAndConversionConfigured.rawFieldCompatibleIndex,
						TypeCheckingAndConversionConfigured.missingFieldIndex,
						TypeCheckingAndConversionConfigured.incompatibleIndex,
						ScaleCheckingConfigured.index, ScaleCheckingConfigured.compatibleIndex,
						ScaleCheckingConfigured.incompatibleIndex
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

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingAndConversionConfigured.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingAndConversionConfigured.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer =
				TypeCheckingAndConversionConfigured.rawFieldCompatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingAndConversionConfigured.missingFieldIndex.bulkIndexer();
		TypeCheckingAndConversionConfigured.dataSets
				.forEach( d -> d.contribute( TypeCheckingAndConversionConfigured.index, typeCheckingMainIndexer,
						TypeCheckingAndConversionConfigured.compatibleIndex, typeCheckingCompatibleIndexer,
						TypeCheckingAndConversionConfigured.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
						TypeCheckingAndConversionConfigured.missingFieldIndex, typeCheckingMissingFieldIndexer ) );

		final BulkIndexer scaleCheckingMainIndexer = ScaleCheckingConfigured.index.bulkIndexer();
		final BulkIndexer scaleCheckingCompatibleIndexer = ScaleCheckingConfigured.compatibleIndex.bulkIndexer();
		ScaleCheckingConfigured.dataSet.contribute( ScaleCheckingConfigured.index, scaleCheckingMainIndexer,
				ScaleCheckingConfigured.compatibleIndex, scaleCheckingCompatibleIndexer );

		singleFieldIndexer.join(
				multiFieldIndexer, inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer,
				analysisMainIndexIndexer, analysisCompatibleIndexIndexer, analysisIncompatibleIndexIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer,
				scaleCheckingMainIndexer, scaleCheckingCompatibleIndexer
		);
	}

	private static <F> RangePredicateTestValues<F> testValues(FieldTypeDescriptor<F> fieldType) {
		return new RangePredicateTestValues<>( fieldType );
	}

	@Nested
	class SingleFieldIT<F> extends SingleFieldConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SingleFieldConfigured<F> extends AbstractPredicateSingleFieldIT<RangePredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}
	}

	@Nested
	class MultiFieldIT<F> extends MultiFieldConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class MultiFieldConfigured<F> extends AbstractPredicateMultiFieldIT<RangePredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "multiField" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).field( otherFieldPath )
					.range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal,
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().fields( fieldPaths ).range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).fields( fieldPaths )
					.range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}
	}

	@Nested
	class InObjectFieldIT<F> extends InObjectFieldConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured<F>
			extends AbstractPredicateFieldInObjectFieldIT<RangePredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
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
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}
	}

	@Nested
	class AnalysisIT extends AnalysisConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class AnalysisConfigured extends AbstractPredicateSimpleAnalysisIT {
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
			return f.range().field( fieldPath ).range( Range.between( matchingParam, matchingParam ) );
		}
	}

	@Nested
	class ScoreIT<F> extends ScoreConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured<F> extends AbstractPredicateFieldScoreIT<RangePredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "score" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().fields( fieldPaths ).range( dataSet.values.matchingRange( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().fields( fieldPaths ).range( dataSet.values.matchingRange( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().fields( fieldPaths ).range( dataSet.values.matchingRange( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).boost( fieldBoost )
					.range( dataSet.values.matchingRange( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).boost( fieldBoost )
					.range( dataSet.values.matchingRange( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost,
				DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return f.range().field( fieldPath ).boost( fieldBoost )
					.range( dataSet.values.matchingRange( matchingDocOrdinal ) ).boost( predicateBoost );
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
			f.range().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:range";
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
			for ( FieldTypeDescriptor<?> fieldType : unsupportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.range().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:range";
		}
	}

	@Nested
	class SearchableIT extends SearchableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SearchableConfigured extends AbstractPredicateSearchableIT {
		private static final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex =
				SimpleMappedIndex.of( root -> new SearchableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableYes" );

		private static final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex =
				SimpleMappedIndex.of( root -> new SearchableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableNo" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( searchableYesIndex, searchableNoIndex, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.range().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:range";
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

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@ParameterizedTest(name = "{1}")
		@MethodSource("params")
		void nullBounds(SimpleMappedIndex<IndexBinding> index, FieldTypeDescriptor<?> fieldType) {
			SearchPredicateFactory f = index.createScope().predicate();

			assertThatThrownBy( () -> f.range().field( fieldPath( index, fieldType ) )
					.range( Range.between( null, null ) ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Invalid range",
							"at least one bound in range predicates must be non-null",
							fieldPath( index, fieldType )
					);
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.range().field( fieldPath ).range( null );
		}
	}

	@Nested
	class TypeCheckingAndConversionIT<F> extends TypeCheckingAndConversionConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class TypeCheckingAndConversionConfigured<F>
			extends AbstractPredicateTypeCheckingAndConversionIT<RangePredicateTestValues<F>, Range<?>> {
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

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex,
						incompatibleIndex, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Range<?> rangeParam) {
			return f.range().field( fieldPath ).range( rangeParam );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Range<?> rangeParam,
				ValueConvert valueConvert) {
			return f.range().field( fieldPath ).range( rangeParam, valueConvert );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				Range<?> rangeParam, ValueConvert valueConvert) {
			return f.range().field( field0Path ).field( field1Path ).range( rangeParam, valueConvert );
		}

		@Override
		protected Range<?> invalidTypeParam() {
			return Range.between( new InvalidType(), new InvalidType() );
		}

		@Override
		protected Range<?> unwrappedMatchingParam(int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return dataSet.values.matchingRange( matchingDocOrdinal );
		}

		@Override
		protected Range<?> wrappedMatchingParam(int matchingDocOrdinal, DataSet<?, RangePredicateTestValues<F>> dataSet) {
			return unwrappedMatchingParam( matchingDocOrdinal, dataSet ).map( ValueWrapper::new );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:range";
		}
	}

	@Nested
	class ScaleCheckingIT extends ScaleCheckingConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScaleCheckingConfigured extends AbstractPredicateScaleCheckingIT {
		private static final DataSet dataSet = new DataSet();

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "scaleChecking_main" );
		private static final SimpleMappedIndex<IndexBinding> compatibleIndex = SimpleMappedIndex.of( IndexBinding::new )
				.name( "scaleChecking_compatible" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( IncompatibleIndexBinding::new )
						.name( "scaleChecking_incompatible" );

		public ScaleCheckingConfigured() {
			super( index, compatibleIndex, incompatibleIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Object matchingParam) {
			return f.range().field( fieldPath ).range( Range.between( matchingParam, matchingParam ) );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:range";
		}
	}
}
