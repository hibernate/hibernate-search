/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils.referenceMatcher;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.search.StubDocumentReferenceTransformer;
import org.hibernate.search.integrationtest.backend.tck.search.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.search.StubObjectLoader;
import org.hibernate.search.integrationtest.backend.tck.search.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.GenericStubMappingSearchTarget;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMock;

public class SearchProjectionIT {
	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void field_noProjections() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query()
				.asProjections()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( query ).hasHitCount( 4 );
	}

	@Test
	public void field_single() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath, fieldType ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( fieldModel.document1Value.indexedValue );
				b.list( fieldModel.document2Value.indexedValue );
				b.list( fieldModel.document3Value.indexedValue );
				b.list( null ); // Empty document
			} );
		}
	}

	@Test
	public void field_single_noClass() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( fieldModel.document1Value.indexedValue );
				b.list( fieldModel.document2Value.indexedValue );
				b.list( fieldModel.document3Value.indexedValue );
				b.list( null ); // Empty document
			} );
		}
	}

	@Test
	public void field_single_validSuperClass() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query()
				.asProjections( searchTarget.projection()
						.field( indexMapping.string1Field.relativeFieldName, CharSequence.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( indexMapping.string1Field.document1Value.indexedValue );
			b.list( indexMapping.string1Field.document2Value.indexedValue );
			b.list( indexMapping.string1Field.document3Value.indexedValue );
			b.list( null ); // Empty document
		} );
	}

	@Test
	public void field_single_invalidProjectionType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "for projection on field" );
		thrown.expectMessage( indexMapping.string1Field.relativeFieldName );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( indexMapping.string1Field.relativeFieldName, Integer.class ).toProjection();
	}

	@Test
	public void field_single_nullClass() {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "must not be null" );
		thrown.expectMessage( "clazz" );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( indexMapping.string1Field.relativeFieldName, null ).toProjection();
	}

	@Test
	public void field_withProjectionConverters() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath, ValueWrapper.class ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( new ValueWrapper<>( fieldModel.document1Value.indexedValue ) );
				b.list( new ValueWrapper<>( fieldModel.document2Value.indexedValue ) );
				b.list( new ValueWrapper<>( fieldModel.document3Value.indexedValue ) );
				b.list( new ValueWrapper<>( null ) ); // Empty document
			} );
		}
	}

	@Test
	public void field_withProjectionConverter_invalidProjectionType() {
		FieldModel<?> fieldModel = indexMapping.supportedFieldWithProjectionConverterModels.get( 0 );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Invalid type" );
		thrown.expectMessage( "for projection on field" );
		thrown.expectMessage( fieldModel.relativeFieldName );

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		searchTarget.projection().field( fieldModel.relativeFieldName, String.class ).toProjection();
	}

	@Test
	public void field_duplicated() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath, fieldType ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( fieldModel.document1Value.indexedValue );
				b.list( fieldModel.document2Value.indexedValue );
				b.list( fieldModel.document3Value.indexedValue );
				b.list( null ); // Empty document
			} );
		}
	}

	@Test
	public void projectionConstants_references() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query;
		DocumentReference document1Reference = reference( INDEX_NAME, DOCUMENT_1 );
		DocumentReference document2Reference = reference( INDEX_NAME, DOCUMENT_2 );
		DocumentReference document3Reference = reference( INDEX_NAME, DOCUMENT_3 );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY );

		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				searchTarget.projection().documentReference().toProjection();
		SearchProjection<DocumentReference> referenceProjection =
				searchTarget.projection().reference().toProjection();
		SearchProjection<DocumentReference> objectProjection =
				searchTarget.projection().object().toProjection();

		query = searchTarget.query()
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1Reference, document1Reference );
			b.list( document2Reference, document2Reference, document2Reference );
			b.list( document3Reference, document3Reference, document3Reference );
			b.list( emptyReference, emptyReference, emptyReference );
		} );
	}

	/**
	 * Test documentReference/reference/object projections as they are likely to be used by mappers,
	 * i.e. with a custom reference transformer and a custom object loader.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3395")
	public void projectionConstants_references_transformed() {
		DocumentReference document1Reference = reference( INDEX_NAME, DOCUMENT_1 );
		DocumentReference document2Reference = reference( INDEX_NAME, DOCUMENT_2 );
		DocumentReference document3Reference = reference( INDEX_NAME, DOCUMENT_3 );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY );
		StubTransformedReference document1TransformedReference = new StubTransformedReference( document1Reference );
		StubTransformedReference document2TransformedReference = new StubTransformedReference( document2Reference );
		StubTransformedReference document3TransformedReference = new StubTransformedReference( document3Reference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject document1LoadedObject = new StubLoadedObject( document1Reference );
		StubLoadedObject document2LoadedObject = new StubLoadedObject( document2Reference );
		StubLoadedObject document3LoadedObject = new StubLoadedObject( document3Reference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document1Reference ) ) )
				.andReturn( document1TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document2Reference ) ) )
				.andReturn( document2TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( document3Reference ) ) )
				.andReturn( document3TransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		EasyMock.expect( objectLoaderMock.load(
				EasyMockUtils.collectionAnyOrderMatcher( Arrays.asList(
						document1TransformedReference, document2TransformedReference,
						document3TransformedReference, emptyTransformedReference
				) )
		) )
				.andReturn( Arrays.asList(
						document1LoadedObject, document2LoadedObject,
						document3LoadedObject, emptyLoadedObject
				) );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );
		SearchQuery<List<?>> query;

		/*
		 * Note to test writers: make sure to assign these projections to variables,
		 * just so that tests do not compile if someone changes the APIs in an incorrect way.
		 */
		SearchProjection<DocumentReference> documentReferenceProjection =
				searchTarget.projection().documentReference().toProjection();
		SearchProjection<StubTransformedReference> referenceProjection =
				searchTarget.projection().reference().toProjection();
		SearchProjection<StubLoadedObject> objectProjection =
				searchTarget.projection().object().toProjection();

		query = searchTarget.query( objectLoaderMock )
				.asProjections(
						documentReferenceProjection,
						referenceProjection,
						objectProjection
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list( document1Reference, document1TransformedReference, document1LoadedObject );
			b.list( document2Reference, document2TransformedReference, document2LoadedObject );
			b.list( document3Reference, document3TransformedReference, document3LoadedObject );
			b.list( emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );

		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void score() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query = searchTarget.query()
				.asProjections( searchTarget.projection().score().toProjection() )
				.predicate( f -> f.match().onField( indexMapping.scoreField.relativeFieldName ).matching( "scorepattern" ).toPredicate() )
				.sort( c -> c.byScore().desc() )
				.build();

		SearchResult<List<?>> result = query.execute();
		Assertions.assertThat( result.getHitCount() ).isEqualTo( 2 );

		Float score1 = (Float) result.getHits().get( 0 ).get( 0 );
		Float score2 = (Float) result.getHits().get( 1 ).get( 0 );

		Assertions.assertThat( score1 ).isNotNull();
		Assertions.assertThat( score2 ).isNotNull();

		Assertions.assertThat( score1 ).isGreaterThan( score2 );
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections.
	 */
	@Test
	public void mixed() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<List<?>> query;

		query = searchTarget.query()
				.asProjections(
						searchTarget.projection().field( indexMapping.string1Field.relativeFieldName, String.class ).toProjection(),
						searchTarget.projection().documentReference().toProjection(),
						searchTarget.projection().field( indexMapping.string2Field.relativeFieldName, String.class ).toProjection()
				)
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasListHitsAnyOrder( b -> {
			b.list(
					indexMapping.string1Field.document1Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_1 ),
					indexMapping.string2Field.document1Value.indexedValue
			);
			b.list(
					indexMapping.string1Field.document2Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_2 ),
					indexMapping.string2Field.document2Value.indexedValue
			);
			b.list(
					indexMapping.string1Field.document3Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_3 ),
					indexMapping.string2Field.document3Value.indexedValue
			);
			b.list(
					null,
					reference( INDEX_NAME, EMPTY ),
					null
			);
		} );
	}

	@Test
	public void field_inFlattenedObject() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath, fieldType ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( fieldModel.document1Value.indexedValue );
				b.list( fieldModel.document2Value.indexedValue );
				b.list( fieldModel.document3Value.indexedValue );
				b.list( null ); // Empty document
			} );
		}
	}

	@Test
	public void field_inNestedObject() {
		Assume.assumeTrue( "Projections on fields within nested object fields are not supported yet", false );
		// TODO HSEARCH-3062 support projections on fields within nested object fields

		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = indexMapping.nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;
			Class<?> fieldType = fieldModel.type;

			query = searchTarget.query()
					.asProjections( searchTarget.projection().field( fieldPath, fieldType ).toProjection() )
					.predicate( f -> f.matchAll().toPredicate() )
					.build();
			assertThat( query ).hasListHitsAnyOrder( b -> {
				b.list( fieldModel.document1Value );
				b.list( fieldModel.document2Value );
				b.list( fieldModel.document3Value );
				b.list( null ); // Empty document
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
	public void error_unknownField() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "unknownField" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asProjections( searchTarget.projection().field( "unknownField", Object.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
	}

	@Test
	public void error_objectField_nested() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "nestedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asProjections( searchTarget.projection().field( "nestedObject", Object.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
	}

	@Test
	public void error_objectField_flattened() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( "flattenedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query()
				.asProjections( searchTarget.projection().field( "flattenedObject", Object.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
	}

	@Test
	public void error_nonProjectable() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( FieldModel<?> fieldModel : indexMapping.nonProjectableSupportedFieldModels ) {
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

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.string1Field.document1Value.write( document );
			indexMapping.string2Field.document1Value.write( document );

			indexMapping.scoreField.document1Value.write( document );

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
			indexMapping.string2Field.document2Value.write( document );

			indexMapping.scoreField.document2Value.write( document );

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
			indexMapping.string2Field.document3Value.write( document );

			indexMapping.scoreField.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<FieldModel<?>> supportedFieldModels;
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels;
		final List<FieldModel<?>> nonProjectableSupportedFieldModels;

		final FieldModel<String> string1Field;
		final FieldModel<String> string2Field;
		final FieldModel<String> scoreField;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root, "", ignored -> { } );
			supportedFieldWithProjectionConverterModels = mapSupportedFields(
					root, "converted_", c -> c.projectionConverter( ValueWrapper.fromIndexFieldConverter() )
			);
			nonProjectableSupportedFieldModels = mapSupportedFields( root, "nonProjectable_",
					c -> c.projectable( Projectable.NO ) );

			string1Field = FieldModel.mapper( String.class, "ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = FieldModel.mapper( String.class, "ddd", "nnn", "yyy" )
					.map( root, "string2" );

			scoreField = FieldModel.mapper( String.class,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ),
					"scorepattern scorepattern", "scorepattern", "xxx" )
					.map( root, "score" );

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
			supportedFieldModels = mapSupportedFields( objectField, "", ignored -> { } );
		}
	}

	private static List<FieldModel<?>> mapSupportedFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
		return Arrays.asList(
				FieldModel
						// Mix capitalized and non-capitalized text on purpose
						.mapper( String.class,
								c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ),
								"Aaron", "george", "Zach" )
						.map( root, prefix + "normalizedString", additionalConfiguration ),
				FieldModel.mapper( String.class, "aaron", "george", "zach" )
						.map( root, prefix + "nonAnalyzedString", additionalConfiguration ),
				FieldModel.mapper( Integer.class, 1, 3, 5 )
						.map( root, prefix + "integer", additionalConfiguration ),
				FieldModel.mapper(
						LocalDate.class,
						LocalDate.of( 2018, 2, 1 ),
						LocalDate.of( 2018, 3, 1 ),
						LocalDate.of( 2018, 4, 1 )
				)
						.map( root, prefix + "localDate", additionalConfiguration ),
				FieldModel.mapper(
						GeoPoint.class,
						GeoPoint.of( 40, 70 ),
						GeoPoint.of( 40, 75 ),
						GeoPoint.of( 40, 80 )
				)
						.map( root, prefix + "geoPoint", additionalConfiguration )
		);
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
					c -> (StandardIndexSchemaFieldTypedContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				Function<IndexSchemaFieldContext, StandardIndexSchemaFieldTypedContext<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return (parent, name, additionalConfiguration) -> {
				IndexSchemaFieldContext untypedContext = parent.field( name );
				StandardIndexSchemaFieldTypedContext<?, F> context = configuration.apply( untypedContext );
				context.projectable( Projectable.YES );
				additionalConfiguration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new FieldModel<>(
						accessor, name, type,
						document1Value, document2Value, document3Value
				);
			};
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
}
