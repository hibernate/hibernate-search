/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior related to type checking and type conversion of DSL arguments
 * for sorts by field value.
 */

class FieldSortTypeCheckingAndConversionIT<F> {

	private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				parameters.add( Arguments.of( fieldType ) );
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
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

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

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

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, missingFieldIndex, incompatibleIndex )
				.setup();

		initData();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withDslConverters_dslConverterEnabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath( fieldTypeDescriptor );

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL,
						fieldTypeDescriptor
				) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL,
						fieldTypeDescriptor
				) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( new ValueWrapper<>( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL, fieldTypeDescriptor ) ) ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withDslConverters_dslConverterDisabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath( fieldTypeDescriptor );

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_1_AND_2_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, EMPTY, DOCUMENT_2, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BETWEEN_DOCUMENT_2_AND_3_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, EMPTY, DOCUMENT_3 );
		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( AFTER_DOCUMENT_3_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ) );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidType_noDslConverter(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope();

		String absoluteFieldPath = getFieldPath( fieldTypeDescriptor );
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidType_withDslConverter(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope();

		String absoluteFieldPath = getFieldWithDslConverterPath( fieldTypeDescriptor );
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withCompatibleIndex_usingField(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldTypeDescriptor );

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ) ), scope );

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_dslConverterEnabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = getFieldPath( fieldTypeDescriptor );

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.field( fieldPath ).asc().missing()
							.use( new ValueWrapper<>( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL,
									fieldTypeDescriptor
							) ) ), scope );
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_dslConverterDisabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldTypeDescriptor );

		query = matchAllQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ), scope );

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex_dslConverterEnabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() ),
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes."
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldTypeDescriptor );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ) ), scope );

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex_dslConverterDisabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() ),
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes."
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath( fieldTypeDescriptor );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ), ValueConvert.NO ), scope );

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
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex_nested(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() ),
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes."
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldInNestedPath( fieldTypeDescriptor );

		query = matchNonEmptyQuery( f -> f.field( fieldPath ).asc().missing()
				.use( getSingleValueForMissingUse( BEFORE_DOCUMENT_1_ORDINAL, fieldTypeDescriptor ) ), scope );

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_dslConverterEnabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath( fieldTypeDescriptor );

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_dslConverterDisabled(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath( fieldTypeDescriptor );

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

	private String getFieldPath(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		return mainIndex.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldInNestedPath(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		return mainIndex.binding().nested.relativeFieldName
				+ '.' + mainIndex.binding().nested.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private String getFieldWithDslConverterPath(StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
		return mainIndex.binding().fieldWithDslConverterModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static void initDocument(IndexBinding indexBinding, DocumentElement document, Integer ordinal) {
		indexBinding.fieldModels.forEach( fieldModel -> addValue( fieldModel, document, ordinal ) );
		indexBinding.fieldWithDslConverterModels.forEach( fieldModel -> addValue( fieldModel, document, ordinal ) );

		DocumentElement nested = document.addObject( indexBinding.nested.self );
		indexBinding.nested.fieldModels.forEach( fieldModel -> addValue( fieldModel, nested, ordinal ) );
		indexBinding.nested.fieldWithDslConverterModels.forEach( fieldModel -> addValue( fieldModel, nested, ordinal ) );
	}

	@SuppressWarnings("unchecked")
	private F getSingleValueForMissingUse(int ordinal, StandardFieldTypeDescriptor<F> fieldTypeDescriptor) {
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
		protected void addIrrelevantOptions(FieldTypeDescriptor<?, ?> fieldType, StandardIndexFieldTypeOptionsStep<?, ?> c) {
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
