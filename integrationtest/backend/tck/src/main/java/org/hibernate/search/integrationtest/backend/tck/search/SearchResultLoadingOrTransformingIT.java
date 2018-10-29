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
import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.ProjectionsSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.easymock.EasyMock;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SearchResultLoadingOrTransformingIT {

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
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void references_noReferenceTransformer() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.matchAll() )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
	}

	@Test
	public void objects_noObjectLoading() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asObjects()
				.predicate( root -> root.matchAll() )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
	}

	@Test
	public void references_referenceTransformer() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );

		Function<DocumentReference, StubTransformedReference> referenceTransformerMock =
				EasyMock.createMock( StubDocumentReferenceTransformer.class );
		ObjectLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock =
				EasyMock.createMock( StubObjectLoader.class );

		SearchQuery<StubTransformedReference> referencesQuery = searchTarget.query(
				sessionContext, referenceTransformerMock, objectLoaderMock
		)
				.asReferences()
				.predicate( root -> root.matchAll() )
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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

		SearchQuery<StubLoadedObject> objectsQuery =
				searchTarget.query( sessionContext, referenceTransformerMock, objectLoaderMock )
						.asObjects()
						.predicate( root -> root.matchAll() )
						.build();

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference );
		EasyMock.expect( objectLoaderMock.load(
				EasyMock.or(
						EasyMock.eq( Arrays.asList( mainTransformedReference, emptyTransformedReference ) ),
						EasyMock.eq( Arrays.asList( emptyTransformedReference, mainTransformedReference ) )
				)
		) )
				.andReturn( Arrays.asList( mainLoadedObject, emptyLoadedObject ) );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );
		assertThat( objectsQuery ).hasHitsExactOrder( mainLoadedObject, emptyLoadedObject );
		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void projection_referencesTransformer_objectLoading() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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

		SearchQuery<List<?>> projectionsQuery =
				searchTarget.query( sessionContext, referenceTransformerMock, objectLoaderMock )
						.asProjections(
								searchTarget.projection().field( "string", String.class ).toProjection(),
								searchTarget.projection().documentReference().toProjection(),
								searchTarget.projection().reference().toProjection(),
								searchTarget.projection().object().toProjection()
						)
						.predicate( root -> root.matchAll() )
						.build();

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		EasyMock.expect( objectLoaderMock.load(
				EasyMock.or(
						EasyMock.eq( Arrays.asList( mainTransformedReference, emptyTransformedReference ) ),
						EasyMock.eq( Arrays.asList( emptyTransformedReference, mainTransformedReference ) )
				)
		) )
				.andReturn( Arrays.asList( mainLoadedObject, emptyLoadedObject ) );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock );
		assertThat( projectionsQuery ).hasProjectionsHitsAnyOrder( b -> {
			b.projection( STRING_VALUE, mainReference, mainTransformedReference, mainLoadedObject );
			b.projection( null, emptyReference, emptyTransformedReference, emptyLoadedObject );
		} );
		EasyMock.verify( referenceTransformerMock, objectLoaderMock );
	}

	@Test
	public void projections_hitTransformer() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		DocumentReference mainReference = reference( INDEX_NAME, MAIN_ID );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY_ID );
		StubTransformedHit mainTransformedHit = new StubTransformedHit( mainReference );
		StubTransformedHit emptyTransformedHit = new StubTransformedHit( emptyReference );

		Function<List<?>, StubTransformedHit> hitTransformerMock = EasyMock.createMock( StubHitTransformer.class );

		SearchQuery<StubTransformedHit> query = searchTarget.query( sessionContext )
				.asProjections(
						hitTransformerMock,
						searchTarget.projection().field( "string", String.class ).toProjection(),
						searchTarget.projection().field( "string_analyzed", String.class ).toProjection(),
						searchTarget.projection().field( "integer", Integer.class ).toProjection(),
						searchTarget.projection().field( "localDate", LocalDate.class ).toProjection(),
						searchTarget.projection().field( "geoPoint", GeoPoint.class ).toProjection(),
						searchTarget.projection().documentReference().toProjection(),
						searchTarget.projection().reference().toProjection(),
						searchTarget.projection().object().toProjection()
				)
				.predicate( root -> root.matchAll() )
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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

		SearchQuery<StubTransformedHit> query =
				searchTarget.query( sessionContext, referenceTransformerMock, objectLoaderMock )
						.asProjections(
								hitTransformerMock,
								searchTarget.projection().field( "string", String.class ).toProjection(),
								searchTarget.projection().documentReference().toProjection(),
								searchTarget.projection().reference().toProjection(),
								searchTarget.projection().object().toProjection()
						)
						.predicate( root -> root.matchAll() )
						.build();

		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( mainReference ) ) )
				.andReturn( mainTransformedReference )
				.times( 2 );
		EasyMock.expect( referenceTransformerMock.apply( referenceMatcher( emptyReference ) ) )
				.andReturn( emptyTransformedReference )
				.times( 2 );
		EasyMock.expect( objectLoaderMock.load(
				EasyMock.or(
						EasyMock.eq( Arrays.asList( mainTransformedReference, emptyTransformedReference ) ),
						EasyMock.eq( Arrays.asList( emptyTransformedReference, mainTransformedReference ) )
				)
		) )
				.andReturn( Arrays.asList( mainLoadedObject, emptyLoadedObject ) );
		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				STRING_VALUE, mainReference, mainTransformedReference, mainLoadedObject
		) ) )
				.andReturn( mainTransformedHit );
		EasyMock.expect( hitTransformerMock.apply( projectionMatcher(
				null, emptyReference, emptyTransformedReference, emptyLoadedObject
		) ) )
				.andReturn( emptyTransformedHit );
		EasyMock.replay( referenceTransformerMock, objectLoaderMock, hitTransformerMock );
		assertThat( query ).hasHitsAnyOrder( mainTransformedHit, emptyTransformedHit );
		EasyMock.verify( referenceTransformerMock, objectLoaderMock, hitTransformerMock );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.matchAll() )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, MAIN_ID, EMPTY_ID );
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
			string = root.field( "string" ).asString().projectable( Projectable.YES ).createAccessor();
			string_analyzed = root.field( "string_analyzed" ).asString()
					.projectable( Projectable.YES )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
					.createAccessor();
			integer = root.field( "integer" ).asInteger().projectable( Projectable.YES ).createAccessor();
			localDate = root.field( "localDate" ).asLocalDate().projectable( Projectable.YES ).createAccessor();
			geoPoint = root.field( "geoPoint" ).asGeoPoint().projectable( Projectable.YES ).createAccessor();
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
			string = objectField.field( "string" ).asString().projectable( Projectable.YES ).createAccessor();
			integer = objectField.field( "integer" ).asInteger().projectable( Projectable.YES ).createAccessor();
		}
	}

}
