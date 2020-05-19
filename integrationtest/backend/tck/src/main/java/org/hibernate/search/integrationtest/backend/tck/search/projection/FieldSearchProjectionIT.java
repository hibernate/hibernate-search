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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FieldSearchProjectionIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );

	@Before
	public void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, incompatibleIndex )
				.setup();

		initData();
	}

	@Test
	public void simple() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, model.type ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null // Empty document
				);
			} );
		}
	}

	@Test
	public void noClass() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SearchQuery<Object> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = scope.query()
					.select( f -> f.field( fieldPath ) )
					.where( f -> f.matchAll() )
					.toQuery();
			assertThat( query ).hasHitsAnyOrder(
					fieldModel.document1Value.indexedValue,
					fieldModel.document2Value.indexedValue,
					fieldModel.document3Value.indexedValue,
					null // Empty document
			);
		}
	}

	@Test
	public void validSuperClass() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<CharSequence> query = scope.query()
				.select( f ->
						f.field( mainIndex.binding().string1Field.relativeFieldName, CharSequence.class )
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				mainIndex.binding().string1Field.document1Value.indexedValue,
				mainIndex.binding().string1Field.document2Value.indexedValue,
				mainIndex.binding().string1Field.document3Value.indexedValue,
				null // Empty document
		);
	}

	@Test
	public void error_nullClass() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( mainIndex.binding().string1Field.relativeFieldName, (Class<? extends Object>) null )
				.toProjection()
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"must not be null",
						"clazz"
				);
	}

	@Test
	public void error_invalidProjectionType_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Class<?> rightType = fieldModel.type;
			Class<?> wrongType = ( rightType.equals( Integer.class ) ) ? Long.class : Integer.class;

			Assertions.assertThatThrownBy(
					() -> scope.projection().field( fieldPath, wrongType ).toProjection()
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid type" )
					.hasMessageContaining( "for projection on field" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void error_invalidProjectionType_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Class<?> rightType = fieldModel.type;
			Class<?> wrongType = ( rightType.equals( Integer.class ) ) ? Long.class : Integer.class;

			Assertions.assertThatThrownBy(
					() -> scope.projection().field( fieldPath, wrongType, ValueConvert.YES ).toProjection()
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid type" )
					.hasMessageContaining( "for projection on field" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( "unknownField", Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"unknownField",
						mainIndex.name()
				);
	}

	@Test
	public void error_objectField_nested() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( "nestedObject", Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"nestedObject",
						mainIndex.name()
				);
	}

	@Test
	public void error_objectField_flattened() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( "flattenedObject", Object.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"flattenedObject",
						mainIndex.name()
				);
	}

	@Test
	public void error_nonProjectable() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedNonProjectableFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			Assertions.assertThatThrownBy( () -> {
				scope.projection().field( fieldPath, fieldType ).toProjection();
			} )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Projections are not enabled for field" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void withProjectionConverters_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			@SuppressWarnings("rawtypes") // The projection DSL only allows to work with raw types, not with parameterized types
					SearchQuery<ValueWrapper> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = scope.query()
					.select( f -> f.field( fieldPath, ValueWrapper.class ) )
					.where( f -> f.matchAll() )
					.toQuery();
			assertThat( query ).hasHitsAnyOrder(
				new ValueWrapper<>( fieldModel.document1Value.indexedValue ),
				new ValueWrapper<>( fieldModel.document2Value.indexedValue ),
				new ValueWrapper<>( fieldModel.document3Value.indexedValue ),
				new ValueWrapper<>( null )
			);
		}
	}

	@Test
	public void withProjectionConverters_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, model.type, ValueConvert.NO ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null // Empty document
				);
			} );
		}
	}

	@Test
	public void withProjectionConverters_projectionConverterDisabled_withoutType() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, ValueConvert.NO ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null // Empty document
				);
			} );
		}
	}

	@Test
	public void error_invalidProjectionType_withProjectionConverter() {
		FieldModel<?> fieldModel = mainIndex.binding().supportedFieldWithProjectionConverterModels.get( 0 );

		StubMappingScope scope = mainIndex.createScope();

		assertThatThrownBy( () -> scope.projection()
				.field( fieldModel.relativeFieldName, String.class )
				.toProjection()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type",
						"for projection on field",
						fieldModel.relativeFieldName
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f ->
										f.composite(
												f.field( fieldPath, model.type ),
												f.field( fieldPath, model.type )
										)
								)
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						Arrays.asList( model.document1Value.indexedValue, model.document1Value.indexedValue ),
						Arrays.asList( model.document2Value.indexedValue, model.document2Value.indexedValue ),
						Arrays.asList( model.document3Value.indexedValue, model.document3Value.indexedValue ),
						Arrays.asList( null, null ) // Empty document
				);
			} );
		}
	}

	@Test
	public void inFlattenedObject() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().flattenedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = mainIndex.binding().flattenedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						scope.query()
								.select(
										f -> f.field( fieldPath, model.type )
								)
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null // Empty document
				);
			} );
		}
	}

	@Test
	public void inNestedObject() {
		StubMappingScope scope = mainIndex.createScope();

		for ( FieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, model.type ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null // Empty document
				);
			} );
		}
	}

	@Test
	public void multivalued() {
		Assume.assumeTrue( "Multi-valued projections are not supported yet", false );
		// TODO HSEARCH-3391 support multi-valued projections
		//  Project on multi-valued field
		//  Project on fields within a multi-valued flattened object
		//  Project on fields within a multi-valued nested object
	}

	@Test
	public void multiIndex_withCompatibleIndex_noProjectionConverter() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, model.type ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null, // Empty document
						model.document1Value.indexedValue // From the "compatible" index
				);
			} );
		}
	}

	@Test
	public void multiIndex_withCompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, ValueWrapper.class ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						new ValueWrapper<>( model.document1Value.indexedValue ),
						new ValueWrapper<>( model.document2Value.indexedValue ),
						new ValueWrapper<>( model.document3Value.indexedValue ),
						new ValueWrapper<>( null ), // Empty document
						new ValueWrapper<>( model.document1Value.indexedValue ) // From the "compatible" index
				);
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.projection().field( fieldPath ),
					"projection on multiple indexes with incompatible types for field " + fieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.select( f -> f.field( fieldPath, model.type, ValueConvert.NO ) )
								.where( f -> f.matchAll() )
								.toQuery()
				).hasHitsAnyOrder(
						model.document1Value.indexedValue,
						model.document2Value.indexedValue,
						model.document3Value.indexedValue,
						null, // Empty document
						model.document1Value.indexedValue // From the "compatible" index
				);
			} );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndex_projectionConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.projection().field( fieldPath ),
					"projection on multiple indexes with incompatible types for field " + fieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndex_projectionConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.projection().field( fieldPath, ValueConvert.NO ),
					"projection on multiple indexes with incompatible types for field " + fieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndex_inNestedObject() {
		StubMappingScope scope = incompatibleIndex.createScope( mainIndex );

		for ( FieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + model.relativeFieldName;

				Assertions.assertThatThrownBy(
						() -> scope.projection().field( fieldPath, ValueConvert.NO ),
						"projection on multiple indexes with incompatible types for field " + fieldPath
				)
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Multiple index conflicting models on nested document paths" )
						.hasMessageContaining( "'" + fieldPath + "'" );
			} );
		}
	}

	private void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );

					mainIndex.binding().string1Field.document1Value.write( document );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
				} )
				.add( DOCUMENT_2, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().supportedFieldWithProjectionConverterModels.forEach( f -> f.document2Value.write( document ) );

					mainIndex.binding().string1Field.document2Value.write( document );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
				} )
				.add( DOCUMENT_3, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
					mainIndex.binding().supportedFieldWithProjectionConverterModels.forEach( f -> f.document3Value.write( document ) );

					mainIndex.binding().string1Field.document3Value.write( document );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
				} )
				.add( EMPTY, document -> { } );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					compatibleIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					compatibleIndex.binding().supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					rawFieldCompatibleIndex.binding().supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );
				} );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldProjectionExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration,
			FieldModelConsumer<FieldProjectionExpectations<?>, FieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			FieldProjectionExpectations<?> expectations = typeDescriptor.getFieldProjectionExpectations().get();
			FieldModel<?> fieldModel = FieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexBinding {
		final List<FieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels = new ArrayList<>();
		final List<FieldModel<?>> supportedNonProjectableFieldModels = new ArrayList<>();

		final FieldModel<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						supportedFieldModels.add( model );
					}
			);
			mapByTypeFields(
					root, "byType_converted_", c -> c.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						supportedFieldWithProjectionConverterModels.add( model );
					}
			);
			mapByTypeFields(
					root, "byType_nonProjectable_", c -> c.projectable( Projectable.NO ),
					(typeDescriptor, expectations, model) -> {
						supportedNonProjectableFieldModels.add( model );
					}
			);

			string1Field = FieldModel.mapper( String.class, "ccc", "mmm", "xxx" )
					.map( root, "string1" );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final List<FieldModel<?>> supportedFieldModels = new ArrayList<>();

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			mapByTypeFields(
					objectField, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						supportedFieldModels.add( model );
					}
			);
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels = new ArrayList<>();

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldWithProjectionConverterModels from IndexBinding,
			 * but with an incompatible projection converter.
			 */
			mapByTypeFields(
					root, "byType_converted_", c -> c.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter<>() ),
					(typeDescriptor, expectations, model) -> {
						supportedFieldWithProjectionConverterModels.add( model );
					}
			);
		}

		private static class IncompatibleProjectionConverter<F> implements
				FromDocumentFieldValueConverter<F, ValueWrapper> {
			@Override
			public ValueWrapper convert(F value, FromDocumentFieldValueConvertContext context) {
				return null;
			}
		}
	}

	private static class IncompatibleIndexBinding {
		final ObjectMapping flattenedObject;

		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexBinding,
			 * but with an incompatible type.
			 */
			forEachTypeDescriptor( typeDescriptor -> {
				StandardFieldMapper<?, IncompatibleFieldModel> mapper;
				if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
					mapper = IncompatibleFieldModel.mapper( context -> context.asLong() );
				}
				else {
					mapper = IncompatibleFieldModel.mapper( context -> context.asInteger() );
				}
				mapper.map( root, "byType_" + typeDescriptor.getUniqueName() );
			} );

			/*
			 * Add object with the same name of nestedObject of IndexBinding,
			 * but we're using here a ObjectFieldStorage.FLATTENED for it.
			 * If we try to project a field within this object,
			 * this will have to lead to an inconsistency exception.
			 */
			flattenedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.FLATTENED );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					type,
					c -> (StandardIndexFieldTypeOptionsStep<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			// Safe, see caller
			FieldProjectionExpectations<F> expectations = typeDescriptor.getFieldProjectionExpectations().get();
			return mapper(
					typeDescriptor.getJavaType(), typeDescriptor::configure,
					expectations.getDocument1Value(), expectations.getDocument2Value(), expectations.getDocument3Value()
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(reference, name) -> new FieldModel<>(
							reference, name, type, document1Value, document2Value, document3Value
					)
			);
		}

		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName, Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(reference, name) -> new IncompatibleFieldModel( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
