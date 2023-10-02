/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues.CENTER_POINT;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests behavior related to type checking and type conversion of DSL arguments
 * for sorts by field value.
 */

class DistanceSortTypeCheckingAndConversionIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String MISSING_FIELD_INDEX_DOCUMENT_1 = "missing_field_1";

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

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

	@Test
	void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

		assertThatQuery( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex() {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchNonEmptyQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

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
	void multiIndex_withMissingFieldIndex_nested() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsDistanceSortWhenNestedFieldMissingInSomeTargetIndexes(),
				"This backend doesn't support distance sorts on a nested field that is missing from some of the target indexes."
		);

		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldInNestedPath();

		query = matchNonEmptyQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

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
	void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'sort:distance'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
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
		return mainIndex.binding().fieldModel.relativeFieldName;
	}

	private String getFieldInNestedPath() {
		return mainIndex.binding().nested.relativeFieldName
				+ '.' + mainIndex.binding().nested.fieldModel.relativeFieldName;
	}

	private String getFieldWithDslConverterPath() {
		return mainIndex.binding().fieldWithDslConverterModel.relativeFieldName;
	}

	private static void initDocument(IndexBinding indexBinding, DocumentElement document, Integer ordinal) {
		addValue( indexBinding.fieldModel, document, ordinal );
		addValue( indexBinding.fieldWithDslConverterModel, document, ordinal );

		DocumentElement nested = document.addObject( indexBinding.nested.self );
		addValue( indexBinding.nested.fieldModel, nested, ordinal );
		addValue( indexBinding.nested.fieldWithDslConverterModel, nested, ordinal );
	}

	private static void addValue(SimpleFieldModel<GeoPoint> fieldModel, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				fieldModel.reference,
				AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal )
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
					addValue( binding.fieldModel, document, BETWEEN_DOCUMENT_1_AND_2_ORDINAL );
					addValue( binding.fieldWithDslConverterModel, document, BETWEEN_DOCUMENT_1_AND_2_ORDINAL );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> initDocument( rawFieldCompatibleIndex.binding(), document,
								BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) );
		BulkIndexer missingFieldIndexer = missingFieldIndex.bulkIndexer()
				.add( MISSING_FIELD_INDEX_DOCUMENT_1, document -> {} );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer, missingFieldIndexer );
	}

	private static class AbstractObjectMapping {
		final SimpleFieldModel<GeoPoint> fieldModel;
		final SimpleFieldModel<GeoPoint> fieldWithDslConverterModel;

		AbstractObjectMapping(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "unconverted", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithDslConverterModel = SimpleFieldModel.mapper( fieldType )
					.map(
							root, "converted", c -> c.sortable( Sortable.YES ),
							additionalConfiguration.andThen(
									c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) )
					);
		}
	}

	private static class IndexBinding extends AbstractObjectMapping {
		private final FirstLevelObjectMapping nested;

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
		final SimpleFieldModel<GeoPoint> fieldModel;
		final SimpleFieldModel<GeoPoint> fieldWithDslConverterModel;

		CompatibleIndexBinding(IndexSchemaElement root) {
			this( root, ignored -> {} );
		}

		CompatibleIndexBinding(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "unconverted", c -> {
						c.sortable( Sortable.YES );
						addIrrelevantOptions( c );
					}, additionalConfiguration );
			fieldWithDslConverterModel = SimpleFieldModel.mapper( fieldType )
					.map(
							root, "converted", c -> {
								c.sortable( Sortable.YES );
								addIrrelevantOptions( c );
							},
							additionalConfiguration.andThen(
									c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) )
					);
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.searchable( Searchable.NO );
			c.projectable( Projectable.YES );
			c.aggregable( Aggregable.YES );
		}
	}

	private static class RawFieldCompatibleIndexBinding extends IndexBinding {
		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModel from IndexBinding,
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
			 * Add fields with the same name as the fieldModel from IndexBinding,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( parent, "unconverted", c -> c.sortable( Sortable.YES ) );
		}
	}

}
