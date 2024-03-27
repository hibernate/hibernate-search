/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
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
class ExistsPredicateBaseIT {
	//CHECKSTYLE:ON

	private static final List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedFieldTypes =
					FieldTypeDescriptor.getAll();

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldConfigured.index,
						InObjectFieldConfigured.mainIndex, InObjectFieldConfigured.missingFieldIndex,
						ScoreConfigured.index,
						InvalidFieldConfigured.index,
						SearchableConfigured.searchableDefaultIndex, SearchableConfigured.searchableYesIndex,
						SearchableConfigured.searchableNoIndex,
						TypeCheckingNoConversionConfigured.index, TypeCheckingNoConversionConfigured.compatibleIndex,
						TypeCheckingNoConversionConfigured.rawFieldCompatibleIndex,
						TypeCheckingNoConversionConfigured.missingFieldIndex,
						TypeCheckingNoConversionConfigured.incompatibleIndex,
						ScaleCheckingConfigured.index, ScaleCheckingConfigured.compatibleIndex,
						ScaleCheckingConfigured.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldConfigured.index.bulkIndexer();
		SingleFieldConfigured.dataSets.forEach( d -> d.contribute( SingleFieldConfigured.index, singleFieldIndexer ) );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldConfigured.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldConfigured.missingFieldIndex.bulkIndexer();
		InObjectFieldConfigured.dataSets
				.forEach( d -> d.contribute( InObjectFieldConfigured.mainIndex, inObjectFieldMainIndexer,
						InObjectFieldConfigured.missingFieldIndex, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSets.forEach( d -> d.contribute( scoreIndexer ) );

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

		final BulkIndexer scaleCheckingMainIndexer = ScaleCheckingConfigured.index.bulkIndexer();
		final BulkIndexer scaleCheckingCompatibleIndexer = ScaleCheckingConfigured.compatibleIndex.bulkIndexer();
		ScaleCheckingConfigured.dataSet.contribute( ScaleCheckingConfigured.index, scaleCheckingMainIndexer,
				ScaleCheckingConfigured.compatibleIndex, scaleCheckingCompatibleIndexer );

		singleFieldIndexer.join(
				inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer, scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer,
				scaleCheckingMainIndexer, scaleCheckingCompatibleIndexer
		);
	}

	private static <F> ExistsPredicateTestValues<F> testValues(FieldTypeDescriptor<F, ?> fieldType) {
		return new ExistsPredicateTestValues<>( fieldType );
	}

	@Nested
	class SingleFieldIT<F> extends SingleFieldConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SingleFieldConfigured<F> extends AbstractPredicateSingleFieldIT<ExistsPredicateTestValues<F>> {

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );
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
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			return f.exists().field( fieldPath );
		}
	}

	@Nested
	class ScoreIT<F> extends ScoreConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured<F> extends AbstractPredicateScoreIT {

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "score" );
		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				DataSet<?> dataSet = new DataSet<>( fieldType );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).constantScore().boost( boost );
		}

		@SuppressWarnings("unchecked")
		private String fieldPath(int matchingDocOrdinal, AbstractPredicateDataSet dataSet) {
			SimpleFieldModelsByType field;
			switch ( matchingDocOrdinal ) {
				case 0:
					field = index.binding().field0;
					break;
				case 1:
					field = index.binding().field1;
					break;
				default:
					throw new IllegalStateException( "This test only works with up to two documents" );
			}
			return field.get( ( (DataSet<F>) dataSet ).fieldType ).relativeFieldName;
		}

		private static class IndexBinding {
			final SimpleFieldModelsByType field0;
			final SimpleFieldModelsByType field1;

			IndexBinding(IndexSchemaElement root) {
				field0 = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root, "field0_" );
				field1 = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root, "field1_" );
			}
		}

		private static class DataSet<F> extends AbstractPerFieldTypePredicateDataSet<F, ExistsPredicateTestValues<F>> {
			protected DataSet(FieldTypeDescriptor<F, ?> fieldType) {
				super( testValues( fieldType ) );
			}

			public void contribute(BulkIndexer scoreIndexer) {
				IndexBinding binding = index.binding();
				scoreIndexer.add( docId( 0 ), routingKey, document -> {
					document.addValue( binding.field0.get( fieldType ).reference, values.value() );
				} );
				scoreIndexer.add( docId( 1 ), routingKey, document -> {
					document.addValue( binding.field1.get( fieldType ).reference, values.value() );
				} );
				scoreIndexer.add( docId( 2 ), routingKey, document -> {} );
			}
		}
	}

	@Nested
	class InObjectFieldIT<F> extends InObjectFieldConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured<F>
			extends AbstractPredicateFieldInObjectFieldIT<ExistsPredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
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
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			if ( matchingDocOrdinal != 0 ) {
				throw new IllegalStateException( "This predicate can only match the first document" );
			}
			return f.exists().field( fieldPath );
		}
	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InvalidFieldConfigured extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		protected InvalidFieldConfigured() {
			super( index );
		}

		@Override
		public void trait_objectField_flattened() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		public void trait_objectField_nested() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		public void use_objectField_flattened() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		public void use_objectField_nested() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.exists().field( fieldPath );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:exists";
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
			return parameters;
		}

		@Override
		void searchable_no_trait(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
				SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
				SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			// The 'exists' predicate actually can be used on unsearchable fields

			String fieldPath = searchableNoIndex.binding().field.get( fieldType ).relativeFieldName;

			assertThat( searchableNoIndex.toApi().descriptor().field( fieldPath ) )
					.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
							.as( "traits of field '" + fieldPath + "'" )
							.contains( predicateTrait() ) );
		}

		@Override
		void searchable_no_use(SimpleMappedIndex<SearchableDefaultIndexBinding> searchableDefaultIndex,
				SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
				SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex, FieldTypeDescriptor<?, ?> fieldType) {
			throw new org.opentest4j.TestAbortedException(
					"The 'exists' predicate actually can be used on unsearchable fields" );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.exists().field( fieldPath );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:exists";
		}
	}

	@Nested
	class TypeCheckingNoConversionIT<F> extends TypeCheckingNoConversionConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class TypeCheckingNoConversionConfigured<F>
			extends AbstractPredicateTypeCheckingNoConversionIT<ExistsPredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_main" );
		private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
				SimpleMappedIndex.<CompatibleIndexBinding>of( root -> new CompatibleIndexBinding( root, supportedFieldTypes ) {
					@Override
					protected void addIrrelevantOptions(FieldTypeDescriptor<?, ?> fieldType,
							SearchableProjectableIndexFieldTypeOptionsStep<?, ?> c) {
						// It's not as easy to find irrelevant options for the "exists" predicate,
						// since "sortable" and "aggregable", and even "projectable" in some cases,
						// may add doc values which may lead to a different implementation of the "exists" predicate.
					}
				} )
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
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
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
		public void multiIndex_withIncompatibleIndex(SimpleMappedIndex<IndexBinding> index,
				SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
				SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
				SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
				SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used when a field relies"
					+ " on different codecs in different indexes" );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal,
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			return f.exists().field( fieldPath );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal, DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate can only target one field at a time" );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:exists";
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
		public void multiIndex_withIncompatibleIndex() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used when a field relies"
					+ " on different codecs in different indexes" );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Object matchingParam) {
			return f.exists().field( fieldPath );
		}

		@Override
		protected String predicateTrait() {
			return "predicate:exists";
		}
	}
}
