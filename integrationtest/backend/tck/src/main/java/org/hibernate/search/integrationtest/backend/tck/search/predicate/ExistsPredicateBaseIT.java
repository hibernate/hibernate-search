/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
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

	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAll();

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldIT.index,
						InObjectFieldIT.mainIndex, InObjectFieldIT.missingFieldIndex,
						ScoreIT.index,
						InvalidFieldIT.index,
						SearchableIT.searchableYesIndex, SearchableIT.searchableNoIndex,
						TypeCheckingNoConversionIT.index, TypeCheckingNoConversionIT.compatibleIndex,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex, TypeCheckingNoConversionIT.missingFieldIndex,
						TypeCheckingNoConversionIT.incompatibleIndex,
						ScaleCheckingIT.index, ScaleCheckingIT.compatibleIndex, ScaleCheckingIT.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldIT.index.bulkIndexer();
		SingleFieldIT.dataSets.forEach( d -> d.contribute( SingleFieldIT.index, singleFieldIndexer ) );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldIT.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldIT.missingFieldIndex.bulkIndexer();
		InObjectFieldIT.dataSets.forEach( d -> d.contribute( InObjectFieldIT.mainIndex, inObjectFieldMainIndexer,
				InObjectFieldIT.missingFieldIndex, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSets.forEach( d -> d.contribute( scoreIndexer ) );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionIT.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionIT.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer =
				TypeCheckingNoConversionIT.rawFieldCompatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingNoConversionIT.missingFieldIndex.bulkIndexer();
		TypeCheckingNoConversionIT.dataSets
				.forEach( d -> d.contribute( TypeCheckingNoConversionIT.index, typeCheckingMainIndexer,
						TypeCheckingNoConversionIT.compatibleIndex, typeCheckingCompatibleIndexer,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
						TypeCheckingNoConversionIT.missingFieldIndex, typeCheckingMissingFieldIndexer ) );

		final BulkIndexer scaleCheckingMainIndexer = ScaleCheckingIT.index.bulkIndexer();
		final BulkIndexer scaleCheckingCompatibleIndexer = ScaleCheckingIT.compatibleIndex.bulkIndexer();
		ScaleCheckingIT.dataSet.contribute( ScaleCheckingIT.index, scaleCheckingMainIndexer,
				ScaleCheckingIT.compatibleIndex, scaleCheckingCompatibleIndexer );

		singleFieldIndexer.join(
				inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer, scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer,
				scaleCheckingMainIndexer, scaleCheckingCompatibleIndexer
		);
	}

	private static <F> ExistsPredicateTestValues<F> testValues(FieldTypeDescriptor<F> fieldType) {
		return new ExistsPredicateTestValues<>( fieldType );
	}

	@Nested
	class SingleFieldIT<F> extends AbstractPredicateSingleFieldIT<ExistsPredicateTestValues<F>> {

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
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			return f.exists().field( fieldPath );
		}
	}

	@Nested
	class ScoreIT<F> extends AbstractPredicateScoreIT {

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "score" );
		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
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
			protected DataSet(FieldTypeDescriptor<F> fieldType) {
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
	class InObjectFieldIT<F> extends AbstractPredicateFieldInObjectFieldIT<ExistsPredicateTestValues<F>> {
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
				DataSet<?, ExistsPredicateTestValues<F>> dataSet) {
			if ( matchingDocOrdinal != 0 ) {
				throw new IllegalStateException( "This predicate can only match the first document" );
			}
			return f.exists().field( fieldPath );
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
		public void objectField_flattened() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		public void objectField_nested() {
			throw new org.opentest4j.TestAbortedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.exists().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:exists";
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
		public void unsearchable(SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex,
				SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex,
				FieldTypeDescriptor<?> fieldType) {
			throw new org.opentest4j.TestAbortedException(
					"The 'exists' predicate actually can be used on unsearchable fields" );
		}

		@Override
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.exists().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:exists";
		}
	}

	@Nested
	class TypeCheckingNoConversionIT<F>
			extends AbstractPredicateTypeCheckingNoConversionIT<ExistsPredicateTestValues<F>> {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "typeChecking_main" );
		private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
				SimpleMappedIndex.<CompatibleIndexBinding>of( root -> new CompatibleIndexBinding( root, supportedFieldTypes ) {
					@Override
					protected void addIrrelevantOptions(FieldTypeDescriptor<?> fieldType,
							StandardIndexFieldTypeOptionsStep<?, ?> c) {
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
		protected String predicateNameInErrorMessage() {
			return "predicate:exists";
		}
	}

	@Nested
	class ScaleCheckingIT extends AbstractPredicateScaleCheckingIT {
		private static final DataSet dataSet = new DataSet();

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "scaleChecking_main" );
		private static final SimpleMappedIndex<IndexBinding> compatibleIndex = SimpleMappedIndex.of( IndexBinding::new )
				.name( "scaleChecking_compatible" );
		private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
				SimpleMappedIndex.of( IncompatibleIndexBinding::new )
						.name( "scaleChecking_incompatible" );

		public ScaleCheckingIT() {
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
		protected String predicateNameInErrorMessage() {
			return "predicate:exists";
		}
	}
}
