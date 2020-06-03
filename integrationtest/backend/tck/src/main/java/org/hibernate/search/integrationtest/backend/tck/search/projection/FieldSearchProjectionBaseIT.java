/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests basic behavior of projections on field value common to all supported types.
 */
public class FieldSearchProjectionBaseIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void simple() {
		StubMappingScope scope = index.createScope();

		for ( FieldModel<?> fieldModel : index.binding().supportedFieldModels ) {
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
		StubMappingScope scope = index.createScope();

		for ( FieldModel<?> fieldModel : index.binding().supportedFieldModels ) {
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

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = index.createScope();

		for ( FieldModel<?> fieldModel : index.binding().supportedFieldModels ) {
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
		StubMappingScope scope = index.createScope();

		for ( FieldModel<?> fieldModel : index.binding().flattenedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = index.binding().flattenedObject.relativeFieldName + "." + model.relativeFieldName;

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
		StubMappingScope scope = index.createScope();

		for ( FieldModel<?> fieldModel : index.binding().nestedObject.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String fieldPath = index.binding().nestedObject.relativeFieldName + "." + model.relativeFieldName;

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

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					index.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					index.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					index.binding().nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
				} )
				.add( DOCUMENT_2, document -> {
					index.binding().supportedFieldModels.forEach( f -> f.document2Value.write( document ) );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					index.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					index.binding().nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
				} )
				.add( DOCUMENT_3, document -> {
					index.binding().supportedFieldModels.forEach( f -> f.document3Value.write( document ) );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					index.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					index.binding().nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
				} )
				.add( EMPTY, document -> { } )
				.join();
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream().forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration,
			FieldModelConsumer<FieldProjectionExpectations<?>, FieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			FieldProjectionExpectations<?> expectations = typeDescriptor.getFieldProjectionExpectations();
			FieldModel<?> fieldModel = FieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexBinding {
		final List<FieldModel<?>> supportedFieldModels = new ArrayList<>();

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						supportedFieldModels.add( model );
					}
			);

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
			FieldProjectionExpectations<F> expectations = typeDescriptor.getFieldProjectionExpectations();
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
}
