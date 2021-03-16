/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
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
public class TermsPredicateBaseIT {

	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<FieldTypeDescriptor<?>> unsupportedFieldTypes = new ArrayList<>();

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

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes(
						SingleFieldIT.index, MultiFieldIT.index,
						MultipleArgumentIT.index,
						NestingIT.mainIndex, NestingIT.missingFieldIndex,
						ScoreIT.index,
						InvalidFieldIT.index, UnsupportedTypeIT.index,
						SearchableIT.searchableYesIndex, SearchableIT.searchableNoIndex,
						ArgumentCheckingIT.index,
						TypeCheckingNoConversionIT.index, TypeCheckingNoConversionIT.compatibleIndex,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex,
						TypeCheckingNoConversionIT.missingFieldIndex,
						TypeCheckingNoConversionIT.incompatibleIndex,
						ScaleCheckingIT.index, ScaleCheckingIT.compatibleIndex, ScaleCheckingIT.incompatibleIndex
				)
				.setup();

		final BulkIndexer singleFieldIndexer = SingleFieldIT.index.bulkIndexer();
		SingleFieldIT.dataSets.forEach( d -> d.contribute( SingleFieldIT.index, singleFieldIndexer ) );

		final BulkIndexer multiFieldIndexer = MultiFieldIT.index.bulkIndexer();
		MultiFieldIT.dataSets.forEach( d -> d.contribute( MultiFieldIT.index, multiFieldIndexer ) );

		final BulkIndexer multipleArgumentIndexer = MultipleArgumentIT.index.bulkIndexer();
		MultipleArgumentIT.dataSets.forEach( d -> d.contribute( MultipleArgumentIT.index, multipleArgumentIndexer ) );

		final BulkIndexer nestingMainIndexer = NestingIT.mainIndex.bulkIndexer();
		final BulkIndexer nestingMissingFieldIndexer = NestingIT.missingFieldIndex.bulkIndexer();
		NestingIT.dataSets.forEach( d -> d.contribute( NestingIT.mainIndex, nestingMainIndexer,
				NestingIT.missingFieldIndex, nestingMissingFieldIndexer
		) );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSets.forEach( d -> d.contribute( ScoreIT.index, scoreIndexer ) );

		final BulkIndexer typeCheckingMainIndexer = TypeCheckingNoConversionIT.index.bulkIndexer();
		final BulkIndexer typeCheckingCompatibleIndexer = TypeCheckingNoConversionIT.compatibleIndex.bulkIndexer();
		final BulkIndexer typeCheckingRawFieldCompatibleIndexer = TypeCheckingNoConversionIT.rawFieldCompatibleIndex
				.bulkIndexer();
		final BulkIndexer typeCheckingMissingFieldIndexer = TypeCheckingNoConversionIT.missingFieldIndex.bulkIndexer();
		TypeCheckingNoConversionIT.dataSets.forEach(
				d -> d.contribute( TypeCheckingNoConversionIT.index, typeCheckingMainIndexer,
						TypeCheckingNoConversionIT.compatibleIndex, typeCheckingCompatibleIndexer,
						TypeCheckingNoConversionIT.rawFieldCompatibleIndex, typeCheckingRawFieldCompatibleIndexer,
						TypeCheckingNoConversionIT.missingFieldIndex, typeCheckingMissingFieldIndexer
				) );

		final BulkIndexer scaleCheckingMainIndexer = ScaleCheckingIT.index.bulkIndexer();
		final BulkIndexer scaleCheckingCompatibleIndexer = ScaleCheckingIT.compatibleIndex.bulkIndexer();
		ScaleCheckingIT.dataSet.contribute( ScaleCheckingIT.index, scaleCheckingMainIndexer,
				ScaleCheckingIT.compatibleIndex, scaleCheckingCompatibleIndexer
		);

		singleFieldIndexer.join(
				multiFieldIndexer, multipleArgumentIndexer, nestingMainIndexer, nestingMissingFieldIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer,
				scaleCheckingMainIndexer, scaleCheckingCompatibleIndexer
		);
	}

	private static <F> TermsPredicateTestValues<F> testValues(FieldTypeDescriptor<F> fieldType) {
		return new TermsPredicateTestValues<>( fieldType );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@RunWith(Parameterized.class)
	public static class SingleFieldIT<F> extends AbstractPredicateSingleFieldIT<TermsPredicateTestValues<F>> {
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

		public SingleFieldIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@RunWith(Parameterized.class)
	public static class MultipleArgumentIT<F> extends AbstractPredicateSingleFieldIT<TermsPredicateTestValues<F>> {
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
						.name( "multipleArgumentField" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public MultipleArgumentIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			if ( dataSet.values.providesNonMatchingArgs() ) {
				return f.terms().field( fieldPath ).matchingAny( dataSet.values.nonMatchingArg( 0 ),
						dataSet.values.matchingArg( matchingDocOrdinal ), dataSet.values.nonMatchingArg( 1 ),
						dataSet.values.nonMatchingArg( 2 ), dataSet.values.nonMatchingArg( 3 ) );
			}

			return f.terms().field( fieldPath ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@RunWith(Parameterized.class)
	public static class MultiFieldIT<F> extends AbstractPredicateMultiFieldIT<TermsPredicateTestValues<F>> {
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
						.name( "multiField" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public MultiFieldIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).field( otherFieldPath )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal) {
			return f.terms().fields( fieldPaths ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).fields( fieldPaths )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@RunWith(Parameterized.class)
	public static class NestingIT<F> extends AbstractPredicateFieldNestingIT<TermsPredicateTestValues<F>> {
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

		public NestingIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( mainIndex, missingFieldIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@RunWith(Parameterized.class)
	public static class ScoreIT<F> extends AbstractPredicateFieldScoreIT<TermsPredicateTestValues<F>> {
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
						.name( "score" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public ScoreIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal) {
			return f.terms().fields( fieldPaths ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost) {
			return f.terms().fields( fieldPaths ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost) {
			return f.terms().fields( fieldPaths ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).boost( fieldBoost )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).boost( fieldBoost )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost) {
			return f.terms().field( fieldPath ).boost( fieldBoost )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
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
			f.terms().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:terms";
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
			f.terms().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:terms";
		}
	}

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
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.terms().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:terms";
		}
	}

	@RunWith(Parameterized.class)
	public static class ArgumentCheckingIT extends AbstractPredicateArgumentCheckingIT {
		private static final List<Object[]> parameters = new ArrayList<>();

		static {
			for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( new Object[] { fieldType } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "argumentChecking" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public ArgumentCheckingIT(FieldTypeDescriptor<?> fieldType) {
			super( index, fieldType );
		}

		@Override
		protected void tryPredicateWithNullMatchingParam(SearchPredicateFactory f, String fieldPath) {
			f.terms().field( fieldPath ).matchingAny( null );
		}
	}

	@RunWith(Parameterized.class)
	public static class TypeCheckingNoConversionIT<F>
			extends AbstractPredicateTypeCheckingNoConversionIT<TermsPredicateTestValues<F>> {
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

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		public TypeCheckingNoConversionIT(DataSet<F, TermsPredicateTestValues<F>> dataSet) {
			super( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.terms().field( fieldPath ).matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal) {
			return f.terms().field( field0Path ).field( field1Path )
					.matchingAny( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:terms";
		}
	}

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
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, Object matchingParam) {
			return f.terms().field( fieldPath ).matchingAny( matchingParam );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:terms";
		}
	}
}
