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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(NestedRunner.class)
public class ExistsPredicateBaseIT {

	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAll();

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
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

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class SingleFieldIT<F> extends AbstractPredicateSingleFieldIT<ExistsPredicateTestValues<F>> {
		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "singleField" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public SingleFieldIT(DataSet<F, ExistsPredicateTestValues<F>> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.exists().field( fieldPath );
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class ScoreIT<F> extends AbstractPredicateScoreIT {
		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?> dataSet = new DataSet<>( fieldType );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "score" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		protected final DataSet<F> dataSet;

		public ScoreIT(DataSet<F> dataSet) {
			super( index, dataSet );
			this.dataSet = dataSet;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).constantScore().boost( boost );
		}

		private String fieldPath(int matchingDocOrdinal) {
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
			return field.get( dataSet.fieldType ).relativeFieldName;
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
	@RunWith(Parameterized.class)
	public static class InObjectFieldIT<F> extends AbstractPredicateFieldInObjectFieldIT<ExistsPredicateTestValues<F>> {
		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex.of( root -> new MissingFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "nesting_missingField" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public InObjectFieldIT(DataSet<F, ExistsPredicateTestValues<F>> dataSet) {
			super( mainIndex, missingFieldIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			if ( matchingDocOrdinal != 0 ) {
				throw new IllegalStateException( "This predicate can only match the first document" );
			}
			return f.exists().field( fieldPath );
		}
	}

	@Nested
	public static class InvalidFieldIT extends AbstractPredicateInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldIT() {
			super( index );
		}

		@Override
		public void objectField_flattened() {
			throw new AssumptionViolatedException( "The 'exists' predicate actually can be used on object fields" );
		}

		@Override
		public void objectField_nested() {
			throw new AssumptionViolatedException( "The 'exists' predicate actually can be used on object fields" );
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
	@RunWith(Parameterized.class)
	public static class SearchableIT extends AbstractPredicateSearchableIT {
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( new Object[] { fieldType } );
			}
		}

		private static final SimpleMappedIndex<SearchableYesIndexBinding> searchableYesIndex =
				SimpleMappedIndex.of( root -> new SearchableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableYes" );

		private static final SimpleMappedIndex<SearchableNoIndexBinding> searchableNoIndex =
				SimpleMappedIndex.of( root -> new SearchableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "searchableNo" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public SearchableIT(FieldTypeDescriptor<?> fieldType) {
			super( searchableYesIndex, searchableNoIndex, fieldType );
		}

		@Override
		public void unsearchable() {
			throw new AssumptionViolatedException( "The 'exists' predicate actually can be used on unsearchable fields" );
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
	@RunWith(Parameterized.class)
	public static class TypeCheckingNoConversionIT<F>
			extends AbstractPredicateTypeCheckingNoConversionIT<ExistsPredicateTestValues<F>> {
		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				DataSet<?, ?> dataSet = new DataSet<>( testValues( fieldType ) );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

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

		public TypeCheckingNoConversionIT(DataSet<F, ExistsPredicateTestValues<F>> dataSet) {
			super( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex, dataSet );
		}

		@Override
		public void multiIndex_withIncompatibleIndex() {
			throw new AssumptionViolatedException( "The 'exists' predicate actually can be used when a field relies"
					+ " on different codecs in different indexes" );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.exists().field( fieldPath );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal) {
			throw new AssumptionViolatedException( "The 'exists' predicate can only target one field at a time" );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:exists";
		}
	}

	@Nested
	public static class ScaleCheckingIT extends AbstractPredicateScaleCheckingIT {
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
			throw new AssumptionViolatedException( "The 'exists' predicate actually can be used when a field relies"
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
