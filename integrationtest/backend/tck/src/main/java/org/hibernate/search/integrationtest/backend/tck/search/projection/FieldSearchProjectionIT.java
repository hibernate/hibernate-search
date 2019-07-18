/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

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
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldSearchProjectionIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping = new RawFieldCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void simple() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, model.type ) )
								.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<Object> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = scope.query()
					.asProjection( f -> f.field( fieldPath ) )
					.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<CharSequence> query = scope.query()
				.asProjection( f ->
						f.field( indexMapping.string1Field.relativeFieldName, CharSequence.class )
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				indexMapping.string1Field.document1Value.indexedValue,
				indexMapping.string1Field.document2Value.indexedValue,
				indexMapping.string1Field.document3Value.indexedValue,
				null // Empty document
		);
	}

	@Test
	public void error_nullClass() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "must not be null" );
		thrown.expectMessage( "clazz" );

		StubMappingScope scope = indexManager.createScope();

		scope.projection().field( indexMapping.string1Field.relativeFieldName, (Class<? extends Object>) null ).toProjection();
	}

	@Test
	public void error_invalidProjectionType_projectionConverterEnabled() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Class<?> rightType = fieldModel.type;
			Class<?> wrongType = ( rightType.equals( Integer.class ) ) ? Long.class : Integer.class;

			SubTest.expectException(
					() -> scope.projection().field( fieldPath, wrongType ).toProjection()
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid type" )
					.hasMessageContaining( "for projection on field" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void error_invalidProjectionType_projectionConverterDisabled() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Class<?> rightType = fieldModel.type;
			Class<?> wrongType = ( rightType.equals( Integer.class ) ) ? Long.class : Integer.class;

			SubTest.expectException(
					() -> scope.projection().field( fieldPath, wrongType, ValueConvert.YES ).toProjection()
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid type" )
					.hasMessageContaining( "for projection on field" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "unknownField" );
		thrown.expectMessage( INDEX_NAME );

		scope.projection().field( "unknownField", Object.class );
	}

	@Test
	public void error_objectField_nested() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "nestedObject" );
		thrown.expectMessage( INDEX_NAME );

		scope.projection().field( "nestedObject", Object.class );
	}

	@Test
	public void error_objectField_flattened() {
		StubMappingScope scope = indexManager.createScope();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "flattenedObject" );
		thrown.expectMessage( INDEX_NAME );

		scope.projection().field( "flattenedObject", Object.class );
	}

	@Test
	public void error_nonProjectable() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedNonProjectableFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			SubTest.expectException( () -> {
				scope.projection().field( fieldPath, fieldType ).toProjection();
			} ).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Projections are not enabled for field" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void withProjectionConverters_projectionConverterEnabled() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			@SuppressWarnings("rawtypes") // The projection DSL only allows to work with raw types, not with parameterized types
					SearchQuery<ValueWrapper> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = scope.query()
					.asProjection( f -> f.field( fieldPath, ValueWrapper.class ) )
					.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, model.type, ValueConvert.NO ) )
								.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, ValueConvert.NO ) )
								.predicate( f -> f.matchAll() )
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
		FieldModel<?> fieldModel = indexMapping.supportedFieldWithProjectionConverterModels.get( 0 );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "for projection on field" );
		thrown.expectMessage( fieldModel.relativeFieldName );

		StubMappingScope scope = indexManager.createScope();

		scope.projection().field( fieldModel.relativeFieldName, String.class ).toProjection();
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f ->
										f.composite(
												f.field( fieldPath, model.type ),
												f.field( fieldPath, model.type )
										)
								)
								.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection(
										f -> f.field( fieldPath, model.type )
								)
								.predicate( f -> f.matchAll() )
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
		StubMappingScope scope = indexManager.createScope();

		for ( FieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = indexMapping.nestedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, model.type ) )
								.predicate( f -> f.matchAll() )
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
	public void multiIndex_withCompatibleIndexManager_noProjectionConverter() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, model.type ) )
								.predicate( f -> f.matchAll() )
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
	public void multiIndex_withCompatibleIndexManager_projectionConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, ValueWrapper.class ) )
								.predicate( f -> f.matchAll() )
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
	public void multiIndex_withRawFieldCompatibleIndexManager_projectionConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"projection on multiple indexes with incompatible types for field " + fieldPath,
					() -> scope.projection().field( fieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_projectionConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						scope.query()
								.asProjection( f -> f.field( fieldPath, model.type, ValueConvert.NO ) )
								.predicate( f -> f.matchAll() )
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
	public void multiIndex_withIncompatibleIndexManager_projectionConverterEnabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"projection on multiple indexes with incompatible types for field " + fieldPath,
					() -> scope.projection().field( fieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndexManager_projectionConverterDisabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"projection on multiple indexes with incompatible types for field " + fieldPath,
					() -> scope.projection().field( fieldPath, ValueConvert.NO )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.string1Field.document1Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document2Value.write( document ) );

			indexMapping.string1Field.document2Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.string1Field.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			compatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			compatibleIndexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			rawFieldCompatibleIndexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		query = compatibleIndexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
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

	private static class IndexMapping {
		final List<FieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels = new ArrayList<>();
		final List<FieldModel<?>> supportedNonProjectableFieldModels = new ArrayList<>();

		final FieldModel<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						supportedFieldModels.add( model );
					}
			);
			mapByTypeFields(
					root, "byType_converted_", c -> c.projectionConverter( ValueWrapper.fromIndexFieldConverter() ),
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

	private static class RawFieldCompatibleIndexMapping {
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels = new ArrayList<>();

		RawFieldCompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldWithProjectionConverterModels from IndexMapping,
			 * but with an incompatible projection converter.
			 */
			mapByTypeFields(
					root, "byType_converted_", c -> c.projectionConverter( new IncompatibleProjectionConverter<>() ),
					(typeDescriptor, expectations, model) -> {
						supportedFieldWithProjectionConverterModels.add( model );
					}
			);
		}

		private static class IncompatibleProjectionConverter<F> implements
				FromDocumentFieldValueConverter<F, ValueWrapper<F>> {
			@Override
			public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
				return superTypeCandidate.isAssignableFrom( ValueWrapper.class );
			}

			@Override
			public ValueWrapper<F> convert(F value, FromDocumentFieldValueConvertContext context) {
				return null;
			}
		}
	}

	private static class IncompatibleIndexMapping {
		IncompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
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
