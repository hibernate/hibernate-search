/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

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
public class FieldSearchProjectionTypeCheckingAndConversionIT<F> {

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

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, incompatibleIndex )
				.setup();

		initData();
	}

	private final FieldTypeDescriptor<F> fieldType;

	public FieldSearchProjectionTypeCheckingAndConversionIT(FieldTypeDescriptor<F> fieldType) {
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

		assertThat( scope.query()
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
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for projection on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void invalidProjectionType_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldPath();

		Class<?> wrongType = FieldTypeDescriptor.getIncompatible( fieldType ).getJavaType();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, wrongType, ValueConvert.NO ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for projection on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void nonProjectable() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getNonProjectableFieldPath();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, fieldType.getJavaType() ).toProjection() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.hasMessageContaining( fieldPath );
	}

	@Test
	public void withProjectionConverters_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = getFieldWithConverterPath();

		assertThat( scope.query()
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

		assertThat( scope.query()
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

		assertThat( scope.query()
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
						"Invalid type",
						"for projection on field",
						fieldPath
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex_noProjectionConverter() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		assertThat( scope.query()
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

		assertThat( scope.query()
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
				.hasMessageContaining( "Multiple conflicting types to build a projection" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = getFieldWithConverterPath();

		assertThat( scope.query()
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
	public void multiIndex_withIncompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> scope.projection().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a projection" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy( () -> scope.projection().field( fieldPath, ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a projection" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_inNestedObject() {
		StubMappingScope scope = incompatibleIndex.createScope( mainIndex );

		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "."
				+ mainIndex.binding().nestedObject.fieldModels.get( fieldType ).relativeFieldName;

		assertThatThrownBy(
				() -> scope.projection().field( fieldPath, ValueConvert.NO ),
				"projection on multiple indexes with incompatible types for field " + fieldPath
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple index conflicting models on nested document paths" )
				.hasMessageContaining( "'" + fieldPath + "'" );
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
				.add( COMPATIBLE_INDEX_DOCUMENT_1,
						document -> initDocument( compatibleIndex.binding(), document, 1 ) );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> rawFieldCompatibleIndex.binding().fieldWithConverterModels
							.forEach( f -> addFieldValue( document, f, 1 ) ) );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;
		final SimpleFieldModelsByType fieldWithProjectionDisabledModels;
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
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() ) );
			fieldWithProjectionDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonProjectable_", c -> c.projectable( Projectable.NO ) );
			fieldWithMultipleValuesModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, root,
					"multiValued_", c -> c.projectable( Projectable.YES ) );

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectFieldStorage.FLATTENED, false );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectFieldStorage.NESTED, false );

			flattenedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_flattenedObject",
					ObjectFieldStorage.FLATTENED, true );
			nestedObjectWithMultipleValues = new ObjectBinding( root, "multiValued_nestedObject",
					ObjectFieldStorage.NESTED, true );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModelsByType fieldModels;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage,
				boolean multiValued) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			if ( multiValued ) {
				objectField.multiValued();
			}
			self = objectField.toReference();
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, objectField,
					"", c -> c.projectable( Projectable.YES ) );
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
				implements FromDocumentFieldValueConverter<Object, ValueWrapper> {
			@Override
			public ValueWrapper convert(Object value, FromDocumentFieldValueConvertContext context) {
				return null;
			}
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
			 * but we're using here a ObjectFieldStorage.FLATTENED for it.
			 * If we try to project a field within this object,
			 * this will have to lead to an inconsistency exception.
			 */
			flattenedObject = new ObjectBinding( root, "nestedObject", ObjectFieldStorage.FLATTENED, false );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			supportedFieldTypes.forEach( typeDescriptor ->
					SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor ) )
							.map( parent, "" + typeDescriptor.getUniqueName(), c -> c.projectable( Projectable.YES ) )
			);
		}
	}
}
