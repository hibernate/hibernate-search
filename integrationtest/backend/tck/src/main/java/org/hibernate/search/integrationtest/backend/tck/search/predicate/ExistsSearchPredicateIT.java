/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LongFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ExistsSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String INCOMPATIBLE_INDEX_DOCUMENT_1 = "incompatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private IncompatibleIndexMapping incompatibleIndexMapping;
	private StubMappingIndexManager incompatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
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
						ctx -> this.incompatibleIndexMapping = new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void exists() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.exists().onField( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * There's no such thing as a "missing" predicate,
	 * but let's check that negating the "exists" predicate works as intended.
	 */
	@Test
	public void missing() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.bool().mustNot( f.exists().onField( absoluteFieldPath ) ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, EMPTY );
		}
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.exists().onField( indexMapping.string1Field.relativeFieldName ) )
						.should( f.exists().onField( indexMapping.string2Field.relativeFieldName ).boostedTo( 7 ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.exists().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 39 ) )
						.should( f.exists().onField( indexMapping.string2Field.relativeFieldName ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void inFlattenedObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.exists().onField( absoluteFieldPath ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * Querying fields of a nested object without a nested predicate should result in no result at all.
	 */
	@Test
	public void inNestedObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.exists().onField( absoluteFieldPath ) )
					.toQuery();

			assertThat( query ).hasNoHits();
		}
	}

	@Test
	public void inNestedPredicate() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.nested().onObjectField( indexMapping.nestedObject.relativeFieldName )
							.nest( f.exists().onField( absoluteFieldPath ) ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	/**
	 * If we require a field not to exist in a nested predicate,
	 * a document will match if *any* of its nested objects lacks the field.
	 */
	@Test
	public void inNestedPredicate_missing() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.nested().onObjectField( indexMapping.nestedObject.relativeFieldName )
							.nest( f.bool().mustNot( f.exists().onField( absoluteFieldPath ) ) ) )
					.toQuery();

			assertThat( query )
					// No match for document 1, since all of its nested objects have this field
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void unknownField() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.exists().onField( "unknown_field" ) )
				.toQuery();

		assertThat( query ).hasNoHits();
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingSearchScope scope = indexManager.createSearchScope( compatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;

				IndexSearchQuery<DocumentReference> query = scope.query()
						.asReference()
						.predicate( f -> f.exists().onField( absoluteFieldPath ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( INDEX_NAME, DOCUMENT_1 );
					b.doc( INDEX_NAME, DOCUMENT_2 );
					b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingSearchScope scope = indexManager.createSearchScope( rawFieldCompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;

				IndexSearchQuery<DocumentReference> query = scope.query()
						.asReference()
						.predicate( f -> f.exists().onField( absoluteFieldPath ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( INDEX_NAME, DOCUMENT_1 );
					b.doc( INDEX_NAME, DOCUMENT_2 );
					b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	/**
	 * Because it's based on a special "field_name" field,
	 * the exists predicate doesn't care about field types.
	 */
	@Test
	public void multiIndex_withIncompatibleIndexManager() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;

				IndexSearchQuery<DocumentReference> query = scope.query()
						.asReference()
						.predicate( f -> f.exists().onField( absoluteFieldPath ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( INDEX_NAME, DOCUMENT_1 );
					b.doc( INDEX_NAME, DOCUMENT_2 );
					b.doc( INCOMPATIBLE_INDEX_NAME, INCOMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.string1Field.document1Value.write( document );

			// Add one object with the values of document 1, and another with the values of document 2
			DocumentElement flattenedObject1 = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject1 ) );
			DocumentElement flattenedObject2 = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );

			// Same for the nested object
			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject1 ) );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.string2Field.document2Value.write( document );

			// Add one empty object, and and another with the values of document 2
			document.addObject( indexMapping.flattenedObject.self );
			DocumentElement flattenedObject2 = document.addObject( indexMapping.flattenedObject.self );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject2 ) );

			// Same for the nested object
			document.addObject( indexMapping.nestedObject.self );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject2 ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.string1Field.document3Value.write( document );

			// Add two empty objects
			document.addObject( indexMapping.flattenedObject.self );
			document.addObject( indexMapping.flattenedObject.self );

			// Same for the nested object
			document.addObject( indexMapping.nestedObject.self );
			document.addObject( indexMapping.nestedObject.self );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			compatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = rawFieldCompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			rawFieldCompatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		workPlan = incompatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( INCOMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			incompatibleIndexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchQuery<DocumentReference> query = indexManager.createSearchScope().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
		query = compatibleIndexManager.createSearchScope().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createSearchScope().query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration,
			FieldModelConsumer<MatchPredicateExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			MatchPredicateExpectations<?> expectations = typeDescriptor.getMatchPredicateExpectations().get();
			ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		final MainFieldModel string1Field;
		final MainFieldModel string2Field;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
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

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			mapByTypeFields(
					objectField, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
					}
			);
		}
	}

	private static class RawFieldCompatibleIndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		RawFieldCompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible DSL converter.
			 */
			mapByTypeFields(
					root, "byType_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						// All types are supported
						supportedFieldModels.add( model );
					}
			);
		}
	}

	private static class IncompatibleIndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		IncompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexMapping,
			 * but with an incompatible type.
			 */
			forEachTypeDescriptor( typeDescriptor -> {
				StandardFieldMapper<?, ? extends ByTypeFieldModel<?>> mapper;
				if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
					mapper = ByTypeFieldModel.mapper( new LongFieldTypeDescriptor() );
				}
				else {
					mapper = ByTypeFieldModel.mapper( new IntegerFieldTypeDescriptor() );
				}
				ByTypeFieldModel<?> model = mapper.map( root, "byType_" + typeDescriptor.getUniqueName() );
				// All types are supported
				supportedFieldModels.add( model );
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
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, String>> configuration,
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
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration) {
			return StandardFieldMapper.of( configuration, (reference, name) -> new IncompatibleFieldModel( name ) );
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
