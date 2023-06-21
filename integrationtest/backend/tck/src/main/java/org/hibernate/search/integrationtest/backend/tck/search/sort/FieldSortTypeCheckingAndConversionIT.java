/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior related to type checking and type conversion of DSL arguments
 * for sorts by field value.
 */
@RunWith(Parameterized.class)
public class FieldSortTypeCheckingAndConversionIT<F> {

	private static List<FieldTypeDescriptor<?>> supportedFieldTypes;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		supportedFieldTypes = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				parameters.add( new Object[] { fieldType } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String MISSING_FIELD_INDEX_DOCUMENT_1 = "missing_field_1";

	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
			SimpleMappedIndex.of( CompatibleIndexBinding::new ).name( "compatible" );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
			SimpleMappedIndex.of( MissingFieldIndexBinding::new ).name( "missingField" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex )
				.setup();

		initData();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSortTypeCheckingAndConversionIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	public void withDslConverters_dslConverterEnabled() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	@Test
	public void withDslConverters_dslConverterDisabled() {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	@Test
	public void unsortable() {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = getNonSortableFieldPath();

		assertThatThrownBy( () -> {
			scope.sort().field( fieldPath );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:field' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	public void invalidType_noDslConverter() {
		StubMappingScope scope = mainIndex.createScope();

		String absoluteFieldPath = getFieldPath();
		Object invalidValueToMatch = new InvalidType();

		assertThatThrownBy(
				() -> scope.sort().field( absoluteFieldPath ).missing()
						.use( invalidValueToMatch ),
				"field() sort with invalid parameter type for missing().use() on field " + absoluteFieldPath
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				) );
	}

	@Test
	public void invalidType_withDslConverter() {
		StubMappingScope scope = mainIndex.createScope();

		String absoluteFieldPath = getFieldWithDslConverterPath();
		Object invalidValueToMatch = new InvalidType();

		assertThatThrownBy(
				() -> scope.sort().field( absoluteFieldPath ).missing()
						.use( invalidValueToMatch ),
				"field() sort with invalid parameter type for missing().use() on field " + absoluteFieldPath
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				) );
	}

	@Test
	public void multiIndex_withCompatibleIndex_usingField() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ), scope );

		/*
		 * Not testing the ordering of results here because some documents have the same value.
		 * It's not what we want to test anyway: we just want to check that fields are correctly
		 * detected as compatible and that no exception is thrown.
		 */
		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
			b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.field( fieldPath ).asc().missing()
							.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ) ), scope );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'dslConverter' differs", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), rawFieldCompatibleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ), ValueConvert.NO ), scope );

		/*
		 * Not testing the ordering of results here because some documents have the same value.
		 * It's not what we want to test anyway: we just want to check that fields are correctly
		 * detected as compatible and that no exception is thrown.
		 */
		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
			b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_dslConverterEnabled() {
		assumeTrue(
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes.",
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() )
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ), scope );

		/*
		 * Not testing the ordering of results here because it's not what we are interested in:
		 * we just want to check that fields are correctly detected as compatible,
		 * that no exception is thrown and that the query is correctly executed on all indexes
		 * with no silent error (HSEARCH-4173).
		 */
		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( missingFieldIndex.typeName(), MISSING_FIELD_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_dslConverterDisabled() {
		assumeTrue(
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes.",
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() )
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ), ValueConvert.NO ), scope );

		/*
		 * Not testing the ordering of results here because it's not what we are interested in:
		 * we just want to check that fields are correctly detected as compatible,
		 * that no exception is thrown and that the query is correctly executed on all indexes
		 * with no silent error (HSEARCH-4173).
		 */
		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( missingFieldIndex.typeName(), MISSING_FIELD_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	/**
	 * Test the behavior when even the <strong>parent</strong> field of the field to sort on is missing,
	 * and that parent field is <strong>nested</strong> in the main index.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_nested() {
		assumeTrue(
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes.",
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() )
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldInNestedPath();

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL ) ), scope );

		/*
		 * Not testing the ordering of results here because it's not what we are interested in:
		 * we just want to check that fields are correctly detected as compatible,
		 * that no exception is thrown and that the query is correctly executed on all indexes
		 * with no silent error (HSEARCH-4173).
		 */
		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( missingFieldIndex.typeName(), MISSING_FIELD_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.field( fieldPath ), scope );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'sort:field'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.field( fieldPath ), scope );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'sort:field'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchAllQuery( sortContributor, mainIndex.createScope() );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll().except( f.id().matching( EMPTY ) ) )
				.sort( sortContributor )
				.toQuery();
	}

	private String getFieldPath() {
		return mainIndex.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldInNestedPath() {
		return mainIndex.binding().nested.relativeFieldName
				+ '.' + mainIndex.binding().nested.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldWithDslConverterPath() {
		return mainIndex.binding().fieldWithDslConverterModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getNonSortableFieldPath() {
		return mainIndex.binding().nonSortableFieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static void initDocument(IndexBinding indexBinding, DocumentElement document, Integer ordinal) {
		indexBinding.fieldModels.forEach( fieldModel -> addValue( fieldModel, document, ordinal ) );
		indexBinding.fieldWithDslConverterModels.forEach( fieldModel -> addValue( fieldModel, document, ordinal ) );

		DocumentElement nested = document.addObject( indexBinding.nested.self );
		indexBinding.nested.fieldModels.forEach( fieldModel -> addValue( fieldModel, nested, ordinal ) );
		indexBinding.nested.fieldWithDslConverterModels.forEach( fieldModel -> addValue( fieldModel, nested, ordinal ) );
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal) {
		F value = fieldTypeDescriptor.getAscendingUniqueTermValues().getSingle().get( ordinal );

		if ( fieldTypeDescriptor instanceof NormalizedStringFieldTypeDescriptor
				&& !TckConfiguration.get().getBackendFeatures().normalizesStringMissingValues() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3387 Remove this once all backends correctly normalize missing value replacements
			value = (F) ( (String) value ).toLowerCase( Locale.ROOT );
		}

		return value;
	}

	private static <F> void addValue(SimpleFieldModel<F> fieldModel, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				fieldModel.reference,
				fieldModel.typeDescriptor.getAscendingUniqueTermValues().getSingle().get( ordinal )
		);
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				.add( DOCUMENT_2, document -> initDocument( mainIndex.binding(), document, DOCUMENT_2_ORDINAL ) )
				.add( EMPTY, document -> initDocument( mainIndex.binding(), document, null ) )
				.add( DOCUMENT_1, document -> initDocument( mainIndex.binding(), document, DOCUMENT_1_ORDINAL ) )
				.add( DOCUMENT_3, document -> initDocument( mainIndex.binding(), document, DOCUMENT_3_ORDINAL ) );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					CompatibleIndexBinding binding = compatibleIndex.binding();
					binding.fieldModels.forEach( fieldModel -> addValue( fieldModel, document, DOCUMENT_1_ORDINAL ) );
					binding.fieldWithDslConverterModels
							.forEach( fieldModel -> addValue( fieldModel, document, DOCUMENT_1_ORDINAL ) );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> initDocument( rawFieldCompatibleIndex.binding(), document, DOCUMENT_1_ORDINAL ) );
		BulkIndexer missingFieldIndexer = missingFieldIndex.bulkIndexer()
				.add( MISSING_FIELD_INDEX_DOCUMENT_1, document -> {} );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer, missingFieldIndexer );
	}

	private static class AbstractObjectMapping {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithDslConverterModels;
		final SimpleFieldModelsByType nonSortableFieldModels;

		AbstractObjectMapping(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldModels = SimpleFieldModelsByType.mapAll(
					supportedFieldTypes,
					root, "", c -> c.sortable( Sortable.YES ), additionalConfiguration
			);
			fieldWithDslConverterModels = SimpleFieldModelsByType.mapAll(
					supportedFieldTypes, root, "converted_",
					c -> c.sortable( Sortable.YES ),
					additionalConfiguration.andThen(
							c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
					)
			);
			nonSortableFieldModels = SimpleFieldModelsByType.mapAll(
					supportedFieldTypes, root, "nonSortable_",
					c -> c.sortable( Sortable.YES ),
					additionalConfiguration.andThen( c -> c.sortable( Sortable.NO ) )
			);
		}
	}

	private static class IndexBinding extends AbstractObjectMapping {
		final FirstLevelObjectMapping nested;

		IndexBinding(IndexSchemaElement root) {
			this( root, ignored -> {} );
		}

		IndexBinding(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( root, additionalConfiguration );
			nested = FirstLevelObjectMapping.create( root, "nested", ObjectStructure.NESTED,
					additionalConfiguration );
		}
	}

	private static class FirstLevelObjectMapping extends AbstractObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static FirstLevelObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			return new FirstLevelObjectMapping( relativeFieldName, objectField, additionalConfiguration );
		}

		private FirstLevelObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			super( objectField, additionalConfiguration );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}

	private static class CompatibleIndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithDslConverterModels;

		CompatibleIndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll(
					supportedFieldTypes,
					root, "", (fieldType, c) -> {
						c.sortable( Sortable.YES );
						addIrrelevantOptions( fieldType, c );
					}
			);
			fieldWithDslConverterModels = SimpleFieldModelsByType.mapAll(
					supportedFieldTypes, root, "converted_", (fieldType, c) -> {
						c.sortable( Sortable.YES );
						c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() );
						addIrrelevantOptions( fieldType, c );
					}
			);
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(FieldTypeDescriptor<?> fieldType, StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.searchable( Searchable.NO );
			c.projectable( Projectable.YES );
			if ( fieldType.isFieldSortSupported() ) {
				c.aggregable( Aggregable.YES );
			}
		}
	}

	private static class RawFieldCompatibleIndexBinding extends IndexBinding {
		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModels from IndexBinding,
			 * but with an incompatible DSL converter.
			 */
			super( root, c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
		}
	}

	private static class MissingFieldIndexBinding {
		MissingFieldIndexBinding(IndexSchemaElement root) {
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModels from IndexBinding,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			supportedFieldTypes.forEach(
					typeDescriptor -> SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor ) )
							.map( parent, "" + typeDescriptor.getUniqueName() )
			);
		}
	}

}
