/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
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
 * Tests behavior related to type checking and type conversion of
 * projections on field value.
 */
@RunWith(Parameterized.class)
public class FieldProjectionTypeCheckingAndConversionIT<F> {

	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAll();

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
			parameters.add( new Object[] { fieldType } );
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

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

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

	private final FieldTypeDescriptor<F> fieldType;

	public FieldProjectionTypeCheckingAndConversionIT(FieldTypeDescriptor<F> fieldType) {
		this.fieldType = fieldType;
	}

	@Test
	public void validSuperClass() {
		StubMappingScope scope = mainIndex.createScope();

		Class<? super F> closestSuperClass = fieldType.getJavaType().getSuperclass();
		if ( closestSuperClass == null ) { // May happen if the field type is an interface, e.g. GeoPoint
			closestSuperClass = Object.class;
		}
		final Class<? super F> validSuperClass = closestSuperClass;

		assertThatQuery( scope.query()
				.select( f -> f.field( getFieldPath(), validSuperClass ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null // Empty document
				);
	}

	@Test
	public void invalidProjectionType_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		Class<?> wrongType = FieldTypeDescriptor.getIncompatible( fieldType ).getJavaType();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, wrongType ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: '" + wrongType.getName() + "'",
						"Expected '" + fieldType.getJavaType().getName() + "' or a supertype",
						"field '" + fieldPath + "'"
				);
	}

	@Test
	public void invalidProjectionType_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		Class<?> wrongType = FieldTypeDescriptor.getIncompatible( fieldType ).getJavaType();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, wrongType, ValueConvert.NO ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: '" + wrongType.getName() + "'",
						"Expected '" + fieldType.getJavaType().getName() + "' or a supertype",
						"field '" + fieldPath + "'"
				);
	}

	@Test
	public void nonProjectable() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getNonProjectableFieldPath();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, fieldType.getJavaType() ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:field' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	public void projectableDefault() {
		assumeFalse(
				"Skipping this test as the backend makes fields projectable by default.",
				TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
		);
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getNonProjectableFieldPath();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, fieldType.getJavaType() ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:field' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void multiValuedField_singleValuedProjection() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = mainIndex.binding().fieldWithMultipleValuesModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, fieldType.getJavaType() ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void singleValuedFieldInMultiValuedObjectField_flattened_singleValuedProjection() {
		String fieldPath = mainIndex.binding().flattenedObjectWithMultipleValues.relativeFieldName
				+ "." + mainIndex.binding().flattenedObjectWithMultipleValues.fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> mainIndex.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void singleValuedFieldInMultiValuedObjectField_nested_singleValuedProjection() {
		String fieldPath = mainIndex.binding().nestedObjectWithMultipleValues.relativeFieldName
				+ "." + mainIndex.binding().nestedObjectWithMultipleValues.fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> mainIndex.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid cardinality for projection on field '" + fieldPath + "'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	public void withProjectionConverters_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, ValueWrapper.class ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						new ValueWrapper<>( getFieldValue( 1 ) ),
						new ValueWrapper<>( getFieldValue( 2 ) ),
						new ValueWrapper<>( getFieldValue( 3 ) ),
						new ValueWrapper<>( null )
				);
	}

	@Test
	public void withProjectionConverters_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType(), ValueConvert.NO ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null // Empty document
				);
	}

	@Test
	public void withProjectionConverters_projectionConverterDisabled_withoutType() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, ValueConvert.NO ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null // Empty document
				);
	}

	@Test
	public void invalidProjectionType_withProjectionConverter() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, String.class )
				.toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: '" + String.class.getName() + "'",
						"Expected '" + ValueWrapper.class.getName() + "' or a supertype",
						"field '" + fieldPath + "'"
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex_noProjectionConverter() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.select( f -> f.field( getFieldPath(), fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null, // Empty document
						getFieldValue( 1 ) // From the "compatible" index
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, ValueWrapper.class ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						new ValueWrapper<>( getFieldValue( 1 ) ),
						new ValueWrapper<>( getFieldValue( 2 ) ),
						new ValueWrapper<>( getFieldValue( 3 ) ),
						new ValueWrapper<>( null ), // Empty document
						new ValueWrapper<>( getFieldValue( 1 ) ) // From the "compatible" index
				);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThatThrownBy( () -> scope.projection().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'projectionConverter' differs:", " vs. "
				);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType(), ValueConvert.NO ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null, // Empty document
						getFieldValue( 1 ) // From the "compatible" index
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, ValueWrapper.class ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						new ValueWrapper<F>( getFieldValue( 1 ) ),
						new ValueWrapper<F>( getFieldValue( 2 ) ),
						new ValueWrapper<F>( getFieldValue( 3 ) ),
						new ValueWrapper<F>( null ), // Empty document
						new ValueWrapper<F>( null ) // From the "missing field" index
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType(), ValueConvert.NO ) )
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasHitsAnyOrder(
						getFieldValue( 1 ),
						getFieldValue( 2 ),
						getFieldValue( 3 ),
						null, // Empty document
						null // From the "missing field" index
				);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> scope.projection().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'projection:field'"
				);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> scope.projection().field( fieldPath, ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'projection:field'"
				);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_inNestedObject() {
		StubMappingScope scope = incompatibleIndex.createScope( mainIndex );

		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "."
				+ mainIndex.binding().nestedObject.fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy( () -> scope.projection().field( fieldPath, ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'nested", "' differs:"
				);
	}

	private String getFieldPath() {
		return mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
	}

	private String getFieldWithConverterPath() {
		return mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;
	}

	private String getNonProjectableFieldPath() {
		return mainIndex.binding().fieldWithProjectionDisabledModels.get( fieldType ).relativeFieldName;
	}

	private String getProjectableDefaultFieldPath() {
		return mainIndex.binding().fieldWithProjectionDisabledModels.get( fieldType ).relativeFieldName;
	}

	private F getFieldValue(int documentNumber) {
		return getFieldValue( fieldType, documentNumber );
	}

	private static <F> F getFieldValue(FieldTypeDescriptor<F> fieldType, int documentNumber) {
		return fieldType.getIndexableValues().getSingle().get( documentNumber - 1 );
	}

	private static <F> void initDocument(IndexBinding binding, DocumentElement document, int documentNumber) {
		binding.fieldModels.forEach( f -> addFieldValue( document, f, documentNumber ) );
		binding.fieldWithConverterModels.forEach( f -> addFieldValue( document, f, documentNumber ) );

		// Note: this object must be single-valued for these tests
		DocumentElement flattenedObject = document.addObject( binding.flattenedObject.self );
		binding.flattenedObject.fieldModels.forEach( f -> addFieldValue( flattenedObject, f, documentNumber ) );

		// Note: this object must be single-valued for these tests
		DocumentElement nestedObject = document.addObject( binding.nestedObject.self );
		binding.nestedObject.fieldModels.forEach( f -> addFieldValue( nestedObject, f, documentNumber ) );
	}

	private static <F> void addFieldValue(DocumentElement documentElement, SimpleFieldModel<F> fieldModel,
			int documentNumber) {
		documentElement.addValue( fieldModel.reference, getFieldValue( fieldModel.typeDescriptor, documentNumber ) );
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> initDocument( mainIndex.binding(), document, 1 ) )
				.add( DOCUMENT_2, document -> initDocument( mainIndex.binding(), document, 2 ) )
				.add( DOCUMENT_3, document -> initDocument( mainIndex.binding(), document, 3 ) )
				.add( EMPTY, document -> { } );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					compatibleIndex.binding().fieldModels
								.forEach( f -> addFieldValue( document, f, 1 ) );
					compatibleIndex.binding().fieldWithConverterModels
							.forEach( f -> addFieldValue( document, f, 1 ) );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> rawFieldCompatibleIndex.binding().fieldWithConverterModels
							.forEach( f -> addFieldValue( document, f, 1 ) ) );
		BulkIndexer missingFieldIndexer = missingFieldIndex.bulkIndexer()
				.add( MISSING_FIELD_INDEX_DOCUMENT_1, document -> { } );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer, missingFieldIndexer );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;
		final SimpleFieldModelsByType fieldWithProjectionDisabledModels;
		final SimpleFieldModelsByType fieldWithDefaultProjectionModels;
		final SimpleFieldModelsByType fieldWithMultipleValuesModels;

		final ObjectBinding flattenedObject;
		final ObjectBinding nestedObject;

		final ObjectBinding flattenedObjectWithMultipleValues;
		final ObjectBinding nestedObjectWithMultipleValues;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", c -> c.projectable( Projectable.YES ) );
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.projectable( Projectable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() ) );
			fieldWithProjectionDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonProjectable_", c -> c.projectable( Projectable.NO ) );
			fieldWithDefaultProjectionModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"projectableDefault_", c -> c.projectable( Projectable.DEFAULT ) );
			fieldWithMultipleValuesModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, root,
					"multiValued_", c -> c.projectable( Projectable.YES ) );

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectStructure.FLATTENED, false );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectStructure.NESTED, false );

			flattenedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_flattenedObject",
					ObjectStructure.FLATTENED, true );
			nestedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_nestedObject",
					ObjectStructure.NESTED, true );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModelsByType fieldModels;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure,
				boolean multiValued) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			self = objectField.toReference();
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, objectField,
					"", c -> c.projectable( Projectable.YES ) );
		}
	}

	private static class CompatibleIndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;

		CompatibleIndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", (fieldType, c) -> {
						c.projectable( Projectable.YES );
						addIrrelevantOptions( fieldType, c );
					} );
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", (fieldType, c) -> {
							c.projectable( Projectable.YES )
								.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
								.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() );
							addIrrelevantOptions( fieldType, c );
					} );
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(FieldTypeDescriptor<?> fieldType, StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.searchable( Searchable.NO );
			if ( fieldType.isFieldSortSupported() ) {
				c.sortable( Sortable.YES );
				c.aggregable( Aggregable.YES );
			}
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final SimpleFieldModelsByType fieldWithConverterModels;

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldWithConverterModel from IndexBinding,
			 * but with an incompatible projection converter.
			 */
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.projectable( Projectable.YES )
							.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter() ) );
		}

		@SuppressWarnings("rawtypes")
		private static class IncompatibleProjectionConverter
				implements FromDocumentValueConverter<Object, ValueWrapper> {
			@Override
			public ValueWrapper fromDocumentValue(Object value, FromDocumentValueConvertContext context) {
				return null;
			}
		}
	}

	private static class MissingFieldIndexBinding {
		MissingFieldIndexBinding(IndexSchemaElement root) {
		}
	}

	private static class IncompatibleIndexBinding {
		final ObjectBinding flattenedObject;

		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldsModels from IndexBinding,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );

			/*
			 * Add object with the same name of nestedObject of IndexBinding,
			 * but we're using a flattened structure.
			 * If we try to project on a field within this object,
			 * it will lead to an inconsistency exception.
			 */
			flattenedObject = new ObjectBinding( root, "nestedObject", ObjectStructure.FLATTENED, false );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			supportedFieldTypes.forEach( typeDescriptor ->
					SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor ) )
							.map( parent, "" + typeDescriptor.getUniqueName(), c -> c.projectable( Projectable.YES ) )
			);
		}
	}
}
