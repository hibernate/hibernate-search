/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class ExistsSearchPredicateIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
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
	public void exists() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.exists().field( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * Fields with docvalues may be optimized and use a different Lucene query.
	 * Make sure to test the optimization as well.
	 */
	@Test
	public void exists_withDocValues() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithDocValuesModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.exists().field( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * There's no such thing as a "missing" predicate,
	 * but let's check that negating the "exists" predicate works as intended.
	 */
	@Test
	public void missing() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.bool().mustNot( f.exists().field( absoluteFieldPath ) ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, EMPTY );
		}
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.exists().field( mainIndex.binding().string1Field.relativeFieldName ) )
						.should( f.exists().field( mainIndex.binding().string2Field.relativeFieldName ).boost( 7 ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.exists().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 39 ) )
						.should( f.exists().field( mainIndex.binding().string2Field.relativeFieldName ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void inFlattenedObject() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().flattenedObject.supportedFieldModels ) {
			String absoluteFieldPath = mainIndex.binding().flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.exists().field( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * Fields with docvalues may be optimized and use a different Lucene query.
	 * Make sure to test the optimization as well.
	 */
	@Test
	public void inFlattenedObject_withDocValues() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().flattenedObject.supportedFieldWithDocValuesModels ) {
			String absoluteFieldPath = mainIndex.binding().flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.exists().field( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void inNestedPredicate_implicit() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.exists().field( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void inNestedPredicate_explicit() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.nested().objectField( mainIndex.binding().nestedObject.relativeFieldName )
							.nest( f.exists().field( absoluteFieldPath ) ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * Fields with docvalues may be optimized and use a different Lucene query.
	 * Make sure to test the optimization as well.
	 */
	@Test
	public void inNestedPredicate_withDocValues() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldWithDocValuesModels ) {
			String absoluteFieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.nested().objectField( mainIndex.binding().nestedObject.relativeFieldName )
							.nest( f.exists().field( absoluteFieldPath ) ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * If we require a field not to exist in a nested predicate,
	 * a document will match if *any* of its nested objects lacks the field.
	 */
	@Test
	public void inNestedPredicate_missing() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = mainIndex.binding().nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.nested().objectField( mainIndex.binding().nestedObject.relativeFieldName )
							.nest( f.bool().mustNot( f.exists().field( absoluteFieldPath ) ) ) )
					.toQuery();

			assertThat( query )
					// No match for document 1, since all of its nested objects have this field
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void unknownField() {
		StubMappingScope scope = mainIndex.createScope();

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "unknown_field" ),
				"exists() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;

				SearchQuery<DocumentReference> query = scope.query()
						.where( f -> f.exists().field( absoluteFieldPath ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( mainIndex.typeName(), DOCUMENT_2 );
					b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;

				SearchQuery<DocumentReference> query = scope.query()
						.where( f -> f.exists().field( absoluteFieldPath ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( mainIndex.typeName(), DOCUMENT_2 );
					b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	/**
	 * The "exists" predicate may take advantage of doc values or norms fields,
	 * which may not be present in every index.
	 * So for now we expect the codec to be identical across indexes.
	 * Theoretically we could be more permissive, but that would require more work and more extensive testing,
	 * and the usefulness of this work is dubious.
	 */
	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().exists().field( fieldPath )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
					) );
		}
	}

	private void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().supportedFieldWithDocValuesModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().string1Field.document1Value.write( document );

					// Add one object with the values of document 1, and another with the values of document 2
					DocumentElement flattenedObject1 = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject1 ) );
					mainIndex.binding().flattenedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document1Value.write( flattenedObject1 ) );
					DocumentElement flattenedObject2 = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );
					// Can't add two values to a sortable field
					//index.binding().flattenedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );

					// Same for the nested object
					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject1 ) );
					mainIndex.binding().nestedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document1Value.write( nestedObject1 ) );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
					mainIndex.binding().nestedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
				} )
				.add( DOCUMENT_2, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().supportedFieldWithDocValuesModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().string2Field.document2Value.write( document );

					// Add one empty object, and and another with the values of document 2
					document.addObject( mainIndex.binding().flattenedObject.self );
					DocumentElement flattenedObject2 = document.addObject( mainIndex.binding().flattenedObject.self );
					mainIndex.binding().flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );
					mainIndex.binding().flattenedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );

					// Same for the nested object
					document.addObject( mainIndex.binding().nestedObject.self );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					mainIndex.binding().nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
					mainIndex.binding().nestedObject.supportedFieldWithDocValuesModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
				} )
				.add( DOCUMENT_3, document -> {
					mainIndex.binding().string1Field.document3Value.write( document );

					// Add two empty objects
					document.addObject( mainIndex.binding().flattenedObject.self );
					document.addObject( mainIndex.binding().flattenedObject.self );

					// Same for the nested object
					document.addObject( mainIndex.binding().nestedObject.self );
					document.addObject( mainIndex.binding().nestedObject.self );
				} )
				.add( EMPTY, document -> { } );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					compatibleIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					rawFieldCompatibleIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
				} );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration,
			FieldModelConsumer<MatchPredicateExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			MatchPredicateExpectations<?> expectations = typeDescriptor.getMatchPredicateExpectations().get();
			ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static void mapByTypeFieldsIfSortSupported(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration,
			FieldModelConsumer<MatchPredicateExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			MatchPredicateExpectations<?> expectations = typeDescriptor.getMatchPredicateExpectations().get();
			// Ignore non-sortable fields
			if ( typeDescriptor.getFieldSortExpectations().isSupported() ) {
				ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
						.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
				consumer.accept( typeDescriptor, expectations, fieldModel );
			}
		} );
	}

	private static class IndexBinding {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedFieldWithDocValuesModels = new ArrayList<>();

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		final MainFieldModel string1Field;
		final MainFieldModel string2Field;

		IndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
					}
			);
			mapByTypeFieldsIfSortSupported(
					root, "byType_withDocValues_", c -> c.sortable( Sortable.YES ),
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldWithDocValuesModels.add( model );
					}
			);
			string1Field = MainFieldModel.mapper(
					"Irving", null, null
			)
					.map( root, "string1" );
			string2Field = MainFieldModel.mapper(
					null, "Auster", null
			)
					.map( root, "string2" );
			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedFieldWithDocValuesModels = new ArrayList<>();

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage )
					.multiValued();
			self = objectField.toReference();
			mapByTypeFields(
					objectField, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
					}
			);
			mapByTypeFieldsIfSortSupported(
					objectField, "byType_withDocValues_", c -> c.sortable( Sortable.YES ),
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldWithDocValuesModels.add( model );
					}
			);
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexBinding,
			 * but with an incompatible DSL converter.
			 */
			mapByTypeFields(
					root, "byType_", c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
					}
			);
		}
	}

	private static class IncompatibleIndexBinding {
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

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return mapper( c -> c.asString(), document1Value, document2Value, document3Value );
		}

		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, String>> configuration,
				String document1Value, String document2Value, String document3Value) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel( reference, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;
		final ValueModel<String> document1Value;
		final ValueModel<String> document2Value;
		final ValueModel<String> document3Value;

		private MainFieldModel(IndexFieldReference<String> reference, String relativeFieldName,
				String document1Value, String document2Value, String document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			ExistsPredicateExpectations<F> expectations = typeDescriptor.getExistsPredicateExpectations();
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel<>( reference, name, expectations )
			);
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;

		private ByTypeFieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				ExistsPredicateExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( reference, expectations.getDocument2Value() );
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<?, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration) {
			return StandardFieldMapper.of( configuration, (reference, name) -> new IncompatibleFieldModel( name ) );
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
