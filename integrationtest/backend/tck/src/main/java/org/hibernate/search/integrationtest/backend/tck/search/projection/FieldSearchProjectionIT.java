/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldSearchProjectionIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String INCOMPATIBLE_FIELD_TYPES_INDEX_NAME = "IndexWithIncompatibleFieldTypes";
	private static final String INCOMPATIBLE_FIELD_CONVERTERS_INDEX_NAME = "IndexWithIncompatibleFieldProjectionConverters";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private IncompatibleFieldTypesIndexMapping incompatibleFieldTypesIndexMapping;
	private StubMappingIndexManager incompatibleFieldTypesIndexManager;

	private IncompatibleFieldProjectionConvertersIndexMapping incompatibleFieldProjectionConvertersIndexMapping;
	private StubMappingIndexManager incompatibleFieldProjectionConvertersIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						"CompatibleMappedType", COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						"MappedTypeWithIncompatibleFieldTypes", INCOMPATIBLE_FIELD_TYPES_INDEX_NAME,
						ctx -> this.incompatibleFieldTypesIndexMapping =
								new IncompatibleFieldTypesIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleFieldTypesIndexManager = indexManager
				)
				.withIndex(
						"MappedTypeWithIncompatibleFieldProjectionConverters", INCOMPATIBLE_FIELD_CONVERTERS_INDEX_NAME,
						ctx -> this.incompatibleFieldProjectionConvertersIndexMapping =
								new IncompatibleFieldProjectionConvertersIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleFieldProjectionConvertersIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void simple() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection( f -> f.field( fieldPath, model.type ).toProjection() )
								.predicate( f -> f.matchAll() )
								.build()
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<Object> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query()
					.asProjection( f -> f.field( fieldPath ).toProjection() )
					.predicate( f -> f.matchAll() )
					.build();
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<CharSequence> query = searchTarget.query()
				.asProjection( f ->
						f.field( indexMapping.string1Field.relativeFieldName, CharSequence.class ).toProjection()
				)
				.predicate( f -> f.matchAll() )
				.build();

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

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( indexMapping.string1Field.relativeFieldName, null ).toProjection();
	}

	@Test
	public void error_invalidProjectionType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "for projection on field" );
		thrown.expectMessage( indexMapping.string1Field.relativeFieldName );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( indexMapping.string1Field.relativeFieldName, Integer.class ).toProjection();
	}

	@Test
	public void error_unknownField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "unknownField" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.projection().field( "unknownField", Object.class );
	}

	@Test
	public void error_objectField_nested() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "nestedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.projection().field( "nestedObject", Object.class );
	}

	@Test
	public void error_objectField_flattened() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "flattenedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.projection().field( "flattenedObject", Object.class );
	}

	@Test
	public void error_nonProjectable() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedNonProjectableFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			SubTest.expectException( () -> {
				searchTarget.projection().field( fieldPath, fieldType ).toProjection();
			} ).assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Projections are not enabled for field" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void withProjectionConverters() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SearchQuery<ValueWrapper> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query()
					.asProjection( f -> f.field( fieldPath, ValueWrapper.class ).toProjection() )
					.predicate( f -> f.matchAll() )
					.build();
			assertThat( query ).hasHitsAnyOrder(
				new ValueWrapper<>( fieldModel.document1Value.indexedValue ),
				new ValueWrapper<>( fieldModel.document2Value.indexedValue ),
				new ValueWrapper<>( fieldModel.document3Value.indexedValue ),
				new ValueWrapper<>( null )
			);
		}
	}

	@Test
	public void error_invalidProjectionType_withProjectionConverter() {
		FieldModel<?> fieldModel = indexMapping.supportedFieldWithProjectionConverterModels.get( 0 );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "for projection on field" );
		thrown.expectMessage( fieldModel.relativeFieldName );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( fieldModel.relativeFieldName, String.class ).toProjection();
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection( f ->
										f.composite(
												f.field( fieldPath, model.type ),
												f.field( fieldPath, model.type )
										)
										.toProjection()
								)
								.predicate( f -> f.matchAll() )
								.build()
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection(
										f -> f.field( fieldPath, model.type ).toProjection()
								)
								.predicate( f -> f.matchAll() )
								.build()
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
		Assume.assumeTrue( "Projections on fields within nested object fields are not supported yet", false );
		// TODO HSEARCH-3062 support projections on fields within nested object fields

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = indexMapping.nestedObject.relativeFieldName + "." + model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection( f -> f.field( fieldPath, model.type ).toProjection() )
								.predicate( f -> f.matchAll() )
								.build()
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
		// TODO support multi-valued projections

		// TODO Project on multi-valued field

		// TODO Project on fields within a multi-valued flattened object

		// TODO Project on fields within a multi-valued nested object
	}

	@Test
	public void multiIndex() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( compatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection( f -> f.field( fieldPath, model.type ).toProjection() )
								.predicate( f -> f.matchAll() )
								.build()
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
	public void multiIndex_withProjectionConverters() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( compatibleIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = model.relativeFieldName;

				assertThat(
						searchTarget.query()
								.asProjection( f -> f.field( fieldPath, ValueWrapper.class ).toProjection() )
								.predicate( f -> f.matchAll() )
								.build()
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
	public void error_multiIndex_incompatibleType() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleFieldTypesIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"projection on multiple indexes with incompatible types for field " + fieldPath,
					() -> searchTarget.projection().field( fieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a projection" )
					.hasMessageContaining( "'" + fieldPath + "'" );
		}
	}

	@Test
	public void error_multiIndex_incompatibleProjectionConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( incompatibleFieldProjectionConvertersIndexManager );

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"projection on multiple indexes with incompatible types for field " + fieldPath,
					() -> searchTarget.projection().field( fieldPath, ValueWrapper.class )
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
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document2Value.write( document ) );

			indexMapping.string1Field.document2Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.string1Field.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<FieldModel<?>> supportedFieldModels;
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels;
		final List<FieldModel<?>> supportedNonProjectableFieldModels;

		final FieldModel<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapByTypeFields(
					root, "supported_", ignored -> { }
			);
			supportedFieldWithProjectionConverterModels = mapByTypeFields(
					root, "supported_converted_", c -> c.projectionConverter( ValueWrapper.fromIndexFieldConverter() )
			);
			supportedNonProjectableFieldModels = mapByTypeFields(
					root, "supported_nonProjectable_", c -> c.projectable( Projectable.NO )
			);

			string1Field = FieldModel.mapper( String.class, "ccc", "mmm", "xxx" )
					.map( root, "string1" );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldAccessor self;
		final List<FieldModel<?>> supportedFieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
			supportedFieldModels = mapByTypeFields(
					objectField, "supported_", ignored -> { }
			);
		}
	}

	private static List<FieldModel<?>> mapByTypeFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration) {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldProjectionExpectations().isPresent() )
				.map( typeDescriptor -> mapByTypeField( root, prefix, typeDescriptor, additionalConfiguration ) )
				.collect( Collectors.toList() );
	}

	private static <F> FieldModel<F> mapByTypeField(IndexSchemaElement parent, String prefix,
			FieldTypeDescriptor<F> typeDescriptor,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration) {
		String name = prefix + typeDescriptor.getUniqueName();
		FieldProjectionExpectations<F> expectations = typeDescriptor.getFieldProjectionExpectations().get(); // Safe, see caller
		return FieldModel.mapper(
				typeDescriptor.getJavaType(), typeDescriptor::configure,
				expectations.getDocument1Value(), expectations.getDocument2Value(), expectations.getDocument3Value()
		)
				.map( parent, name, additionalConfiguration );
	}

	private static List<IncompatibleFieldModel<?>> mapByTypeSupportedIncompatibleFields(IndexSchemaElement root, String prefix,
			BiFunction<FieldTypeDescriptor<?>, IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, ?>> configuration) {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getFieldProjectionExpectations().isPresent() )
				.map( typeDescriptor -> mapByTypeIncompatibleField( root, prefix, typeDescriptor, configuration ) )
				.collect( Collectors.toList() );
	}

	private static <F> IncompatibleFieldModel<?> mapByTypeIncompatibleField(IndexSchemaElement parent, String prefix,
			FieldTypeDescriptor<F> typeDescriptor,
			BiFunction<FieldTypeDescriptor<?>, IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, ?>> configuration) {
		String name = prefix + typeDescriptor.getUniqueName();
		return IncompatibleFieldModel.mapper(
				context -> configuration.apply( typeDescriptor, context )
		)
				.map( parent, name );
	}

	private static class ValueModel<F> {
		private final IndexFieldAccessor<F> accessor;
		final F indexedValue;

		private ValueModel(IndexFieldAccessor<F> accessor, F indexedValue) {
			this.accessor = accessor;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			accessor.write( target, indexedValue );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					type,
					c -> (StandardIndexFieldTypeContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(accessor, name) -> new FieldModel<>(
							accessor, name, type, document1Value, document2Value, document3Value
					)
			);
		}

		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName, Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}

	private static class IncompatibleFieldTypesIndexMapping {
		final List<IncompatibleFieldModel<?>> fieldModels;

		IncompatibleFieldTypesIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible type.
			 */
			fieldModels = mapByTypeSupportedIncompatibleFields(
					root, "supported_",
					(type, context) -> {
						// Just try to pick a different, also supported type
						if ( Integer.class.equals( type.getJavaType() ) ) {
							return context.as( Long.class ).projectable( Projectable.YES );
						}
						else {
							return context.as( Integer.class ).projectable( Projectable.YES );
						}
					}
			);
		}
	}

	private static class IncompatibleFieldProjectionConvertersIndexMapping {
		final List<IncompatibleFieldModel<?>> fieldModels;

		IncompatibleFieldProjectionConvertersIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldWithProjectionConverterModels from IndexMapping,
			 * but with an incompatible projection converter.
			 */
			fieldModels = mapByTypeSupportedIncompatibleFields(
					root, "supported_converted_",
					(type, context) -> {
						// Just add a different, incompatible projection converter
						return type.configure( context )
								.projectionConverter( new IncompatibleProjectionConverter<>() );
					}
			);
		}

		private class IncompatibleProjectionConverter<F> implements
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

	private static class IncompatibleFieldModel<F> {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel<F>> mapper(Class<F> type) {
			return mapper(
					c -> (StandardIndexFieldTypeContext<?, F>) c.as( type )
			);
		}

		static <F> StandardFieldMapper<F, IncompatibleFieldModel<F>> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(accessor, name) -> new IncompatibleFieldModel<>( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
