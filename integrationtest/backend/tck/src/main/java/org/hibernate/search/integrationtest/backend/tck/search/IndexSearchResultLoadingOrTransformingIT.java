/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils.projectionMatcher;
import static org.hibernate.search.util.impl.integrationtest.common.EasyMockUtils.referenceMatcher;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.easymock.EasyMock;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.GenericStubMappingSearchTarget;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IndexSearchResultLoadingOrTransformingIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String MAIN_ID = "main";
	private static final String EMPTY_ID = "empty";

	private static final Integer INTEGER_VALUE = 42;
	private static final String STRING_VALUE = "string";
	private static final String STRING_ANALYZED_VALUE = "analyzed string";
	private static final LocalDate LOCAL_DATE_VALUE = LocalDate.of( 2018, 2, 1 );
	private static final GeoPoint GEO_POINT_VALUE = GeoPoint.of( 42.0, -42.0 );
	private static final Integer NESTED_OBJECT_INTEGER_VALUE = 142;
	private static final String NESTED_OBJECT_STRING_VALUE = "nested object string";
	private static final Integer FLATTENED_OBJECT_INTEGER_VALUE = 242;
	private static final String FLATTENED_OBJECT_STRING_VALUE = "flattened object string";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void references_noReferenceTransformer() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
	}

	@Test
	public void objects_noObjectLoading() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asObject()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
	}

	@Test
	public void references_referenceTransformer() {
		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );

		IndexSearchQuery<StubTransformedReference> referencesQuery = searchTarget.query( objectLoaderMock )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );
		assertThat( referencesQuery ).hasHitsAnyOrder( mainTransformedReference, emptyTransformedReference );
		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void objects_referencesTransformer_objectLoading() {
		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference );
		StubMapperUtils.expectLoad(
				objectLoaderMock,
				c -> c.load( mainTransformedReference, mainLoadedObject )
						.load( emptyTransformedReference, emptyLoadedObject )
		);
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );

		IndexSearchQuery<StubLoadedObject> objectsQuery = searchTarget.query( objectLoaderMock )
				.asObject()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( objectsQuery ).hasHitsExactOrder( mainLoadedObject, emptyLoadedObject );

		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void projection_referencesTransformer_objectLoading() {
		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		StubMapperUtils.expectLoad(
				objectLoaderMock,
				c -> c.load( mainTransformedReference, mainLoadedObject )
						.load( emptyTransformedReference, emptyLoadedObject )
		);
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );

		IndexSearchQuery<List<?>> projectionsQuery = searchTarget.query( objectLoaderMock )
				.asProjection( f ->
						f.composite(
								f.field( "string", String.class ),
								f.documentReference(),
								f.reference(),
								f.object()
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( projectionsQuery ).hasListHitsAnyOrder( b -> {
			b.list( STRING_VALUE, mainReference, mainTransformedReference, mainLoadedObject );
			b.list( null, emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );

		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void projections_hitTransformer() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedHit mainTransformedHit = new StubTransformedHit( mainReference );
		StubTransformedHit emptyTransformedHit = new StubTransformedHit( emptyReference );

		Function<List<?>, StubTransformedHit> hitTransformerMock = EasyMock.createMock( StubHitTransformer.class );

		IndexSearchQuery<StubTransformedHit> query = searchTarget.query()
				.asProjection( f ->
						f.composite(
								hitTransformerMock,
								f.field( "string", String.class ).toProjection(),
								f.field( "string_analyzed", String.class ).toProjection(),
								f.field( "integer", Integer.class ).toProjection(),
								f.field( "localDate", LocalDate.class ).toProjection(),
								f.field( "geoPoint", GeoPoint.class ).toProjection(),
								f.documentReference().toProjection(),
								f.reference().toProjection(),
								f.object().toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.build();

		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				STRING_VALUE, STRING_ANALYZED_VALUE, INTEGER_VALUE, LOCAL_DATE_VALUE, GEO_POINT_VALUE,
				mainReference, mainReference, mainReference
		) ) )
				.andReturn( mainTransformedHit );
		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				null, null, null, null, null,
				emptyReference, emptyReference, emptyReference
		) ) )
				.andReturn( emptyTransformedHit );
		EasyMock.replay( hitTransformerMock );
		assertThat( query ).hasHitsAnyOrder( mainTransformedHit, emptyTransformedHit );
		EasyMock.verify( hitTransformerMock );
	}

	@Test
	public void projections_hitTransformer_referencesTransformer_objectLoading() {
		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedHit mainTransformedHit = new StubTransformedHit( mainReference );
		StubTransformedHit emptyTransformedHit = new StubTransformedHit( emptyReference );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );
		Function<List<?>, StubTransformedHit> hitTransformerMock = EasyMock.createMock( StubHitTransformer.class );

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		StubMapperUtils.expectLoad(
				objectLoaderMock,
				c -> c.load( mainTransformedReference, mainLoadedObject )
						.load( emptyTransformedReference, emptyLoadedObject )
		);
		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				STRING_VALUE, mainReference, mainTransformedReference, mainLoadedObject
		) ) )
				.andReturn( mainTransformedHit );
		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				null, emptyReference, emptyTransformedReference, emptyLoadedObject
		) ) )
				.andReturn( emptyTransformedHit );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock, hitTransformerMock );

		GenericStubMappingSearchTarget<StubTransformedReference, StubLoadedObject> searchTarget =
				indexManager.createSearchTarget( referenceTransformerMock );

		IndexSearchQuery<StubTransformedHit> query = searchTarget.query( objectLoaderMock )
				.asProjection( f ->
						f.composite(
								hitTransformerMock,
								f.field( "string", String.class ).toProjection(),
								f.documentReference().toProjection(),
								f.reference().toProjection(),
								f.object().toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasHitsAnyOrder( mainTransformedHit, emptyTransformedHit );

		EasyMock.verify( referenceTransformerMock, objectLoaderMock, hitTransformerMock );
	}

	@Test
	public void countQuery() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();

		assertEquals( 2L, query.executeCount() );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE ) )
				.build();

		assertEquals( 1L, query.executeCount() );

		// Using setFirstResult/setMaxResult should not affect the count
		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();

		query.setFirstResult( 1L );
		assertEquals( 2L, query.executeCount() );

		query.setFirstResult( 0L );
		query.setMaxResults( 1L );
		assertEquals( 2L, query.executeCount() );
	}

	@Test
	public void countQueryWithProjection() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		IndexSearchQuery<String> query = searchTarget.query()
				.asProjection( f -> f.field( "string", String.class ) )
				.predicate( f -> f.matchAll() )
				.build();

		assertEquals( 2L, query.executeCount() );

		query = searchTarget.query()
				.asProjection( f -> f.field( "string", String.class ) )
				.predicate( f -> f.match().onField( "string" ).matching( STRING_VALUE ) )
				.build();

		assertEquals( 1L, query.executeCount() );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( MAIN_ID ), document -> {
			indexAccessors.string.write( document, STRING_VALUE );
			indexAccessors.string_analyzed.write( document, STRING_ANALYZED_VALUE );
			indexAccessors.integer.write( document, INTEGER_VALUE );
			indexAccessors.localDate.write( document, LOCAL_DATE_VALUE );
			indexAccessors.geoPoint.write( document, GEO_POINT_VALUE );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, FLATTENED_OBJECT_STRING_VALUE );
			indexAccessors.flattenedObject.integer.write( flattenedObject, FLATTENED_OBJECT_INTEGER_VALUE );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, NESTED_OBJECT_STRING_VALUE );
			indexAccessors.nestedObject.integer.write( nestedObject, NESTED_OBJECT_INTEGER_VALUE );
		} );

		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		IndexSearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> string_analyzed;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;
		final IndexFieldAccessor<GeoPoint> geoPoint;
		final ObjectAccessors flattenedObject;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.createAccessor();
			string_analyzed = root.field(
					"string_analyzed",
					f -> f.asString()
							.projectable( Projectable.YES )
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.createAccessor();
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.createAccessor();
			localDate = root.field(
					"localDate",
					f -> f.asLocalDate().projectable( Projectable.YES )
			)
					.createAccessor();
			geoPoint = root.field(
					"geoPoint",
					f -> f.asGeoPoint().projectable( Projectable.YES )
			)
					.createAccessor();
			IndexSchemaObjectField flattenedObjectField =
					root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectAccessors( nestedObjectField );
		}
	}

	private static class ObjectAccessors {
		final IndexObjectFieldAccessor self;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<String> string;

		ObjectAccessors(IndexSchemaObjectField objectField) {
			self = objectField.createAccessor();
			string = objectField.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.createAccessor();
			integer = objectField.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.createAccessor();
		}
	}

}
