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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(NestedRunner.class)
public class RegexpPredicateBaseIT {

	private static final List<FieldTypeDescriptor<String>> supportedFieldTypes = new ArrayList<>();
	private static final List<FieldTypeDescriptor<?>> unsupportedFieldTypes = new ArrayList<>();
	static {
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( String.class.equals( fieldType.getJavaType() ) ) {
				@SuppressWarnings("unchecked")
				FieldTypeDescriptor<String> casted = (FieldTypeDescriptor<String>) fieldType;
				supportedFieldTypes.add( casted );
			}
			else {
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
		SingleFieldIT.dataSets.forEach( d -> d.contribute( SingleFieldIT.index, singleFieldIndexer ) );

		final BulkIndexer multiFieldIndexer = MultiFieldIT.index.bulkIndexer();
		MultiFieldIT.dataSets.forEach( d -> d.contribute( MultiFieldIT.index, multiFieldIndexer ) );

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldIT.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldIT.missingFieldIndex.bulkIndexer();
		InObjectFieldIT.dataSets.forEach( d -> d.contribute( InObjectFieldIT.mainIndex, inObjectFieldMainIndexer,
				InObjectFieldIT.missingFieldIndex, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSets.forEach( d -> d.contribute( ScoreIT.index, scoreIndexer ) );

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

		singleFieldIndexer.join(
				multiFieldIndexer, inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer,
				scoreIndexer,
				typeCheckingMainIndexer, typeCheckingCompatibleIndexer,
				typeCheckingRawFieldCompatibleIndexer, typeCheckingMissingFieldIndexer
		);
	}

	private static RegexpPredicateTestValues testValues(FieldTypeDescriptor<String> fieldType) {
		return new RegexpPredicateTestValues( fieldType );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class SingleFieldIT extends AbstractPredicateSingleFieldIT<RegexpPredicateTestValues> {
		private static final List<DataSet<String, RegexpPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String> fieldType : supportedFieldTypes ) {
				DataSet<String, RegexpPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
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

		public SingleFieldIT(DataSet<String, RegexpPredicateTestValues> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class MultiFieldIT extends AbstractPredicateMultiFieldIT<RegexpPredicateTestValues> {
		private static final List<DataSet<String, RegexpPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String> fieldType : supportedFieldTypes ) {
				DataSet<String, RegexpPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
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

		public MultiFieldIT(DataSet<String, RegexpPredicateTestValues> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
				String otherFieldPath, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).field( otherFieldPath )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths, int matchingDocOrdinal) {
			return f.regexp().fields( fieldPaths ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
				String[] fieldPaths, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).fields( fieldPaths )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class InObjectFieldIT extends AbstractPredicateFieldInObjectFieldIT<RegexpPredicateTestValues> {
		private static final List<DataSet<?, ?>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String> fieldType : supportedFieldTypes ) {
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

		public InObjectFieldIT(DataSet<String, RegexpPredicateTestValues> dataSet) {
			super( mainIndex, missingFieldIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class ScoreIT extends AbstractPredicateFieldScoreIT<RegexpPredicateTestValues> {
		private static final List<DataSet<String, RegexpPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String> fieldType : supportedFieldTypes ) {
				DataSet<String, RegexpPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
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

		public ScoreIT(DataSet<String, RegexpPredicateTestValues> dataSet) {
			super( index, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal) {
			return f.regexp().fields( fieldPaths ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithPredicateLevelBoost(SearchPredicateFactory f, String[] fieldPaths,
				int matchingDocOrdinal, float predicateBoost) {
			return f.regexp().fields( fieldPaths ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndPredicateLevelBoost(SearchPredicateFactory f,
				String[] fieldPaths, int matchingDocOrdinal, float predicateBoost) {
			return f.regexp().fields( fieldPaths ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) )
					.constantScore().boost( predicateBoost );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoost(SearchPredicateFactory f, String fieldPath,
				float fieldBoost, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndConstantScore(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithFieldLevelBoostAndPredicateLevelBoost(SearchPredicateFactory f,
				String fieldPath, float fieldBoost, int matchingDocOrdinal, float predicateBoost) {
			return f.regexp().field( fieldPath ).boost( fieldBoost )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) ).boost( predicateBoost );
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
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.regexp().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:regexp";
		}
	}

	@Nested
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
			f.regexp().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:regexp";
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
		protected void tryPredicate(SearchPredicateFactory f, String fieldPath) {
			f.regexp().field( fieldPath );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:regexp";
		}
	}

	@Nested
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
			f.regexp().field( fieldPath ).matching( null );
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class TypeCheckingNoConversionIT
			extends AbstractPredicateTypeCheckingNoConversionIT<RegexpPredicateTestValues> {
		private static final List<DataSet<String, RegexpPredicateTestValues>> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<String> fieldType : supportedFieldTypes ) {
				DataSet<String, RegexpPredicateTestValues> dataSet = new DataSet<>( testValues( fieldType ) );
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

		public TypeCheckingNoConversionIT(DataSet<String, RegexpPredicateTestValues> dataSet) {
			super( index, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex, dataSet );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal) {
			return f.regexp().field( fieldPath ).matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
				int matchingDocOrdinal) {
			return f.regexp().field( field0Path ).field( field1Path )
					.matching( dataSet.values.matchingArg( matchingDocOrdinal ) );
		}

		@Override
		protected String predicateNameInErrorMessage() {
			return "predicate:regexp";
		}
	}
}
