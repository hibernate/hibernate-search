/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.stub.MapperMockUtils.expectHitMapping;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings("unchecked") // Mocking parameterized types
public class SearchQueryResultLoadingOrTransformingIT {

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
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3578")
	public void defaultResultType() {
		DocumentReference mainReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyReference = reference( index.typeName(), EMPTY_ID );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatQuery( objectsQuery ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatHits( hitsUsingScroll( objectsQuery ) ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void defaultResultType_entityLoadingTimeout() {
		DocumentReference mainReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyReference = reference( index.typeName(), EMPTY_ID );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.failAfter( 1000L, TimeUnit.HOURS )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatQuery( objectsQuery ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatHits( hitsUsingScroll( objectsQuery ) ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void selectEntityReference_noReferenceTransformer() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.selectEntityReference()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), MAIN_ID, EMPTY_ID );

		// check the same for the scroll API
		assertThatHits( hitsUsingScroll( query ) )
				.hasDocRefHitsAnyOrder( index.typeName(), MAIN_ID, EMPTY_ID );
	}

	@Test
	public void selectEntity_noEntityLoading() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.selectEntity()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), MAIN_ID, EMPTY_ID );

		// check the same for the scroll API
		assertThatHits( hitsUsingScroll( query ) )
				.hasDocRefHitsAnyOrder( index.typeName(), MAIN_ID, EMPTY_ID );
	}

	@Test
	public void selectEntityReference_referenceTransformer() {
		DocumentReference mainReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyReference = reference( index.typeName(), EMPTY_ID );
		StubTransformedReference mainTransformedReference = new StubTransformedReference( mainReference );
		StubTransformedReference emptyTransformedReference = new StubTransformedReference( emptyReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				index.createGenericScope();
		SearchQuery<StubTransformedReference> referencesQuery = scope.query( loadingContextMock )
				.selectEntityReference()
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.entityReference( mainReference, mainTransformedReference )
						.entityReference( emptyReference, emptyTransformedReference )
		);
		assertThatQuery( referencesQuery ).hasHitsAnyOrder( mainTransformedReference, emptyTransformedReference );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.entityReference( mainReference, mainTransformedReference )
						.entityReference( emptyReference, emptyTransformedReference )
		);
		assertThatHits( hitsUsingScroll( referencesQuery ) ).hasHitsAnyOrder( mainTransformedReference, emptyTransformedReference );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void selectEntity_referencesTransformer_entityLoading() {
		DocumentReference mainReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyReference = reference( index.typeName(), EMPTY_ID );
		StubLoadedObject mainLoadedObject = new StubLoadedObject( mainReference );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.selectEntity()
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatQuery( objectsQuery ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainLoadedObject )
						.load( emptyReference, emptyLoadedObject )
		);
		assertThatHits( hitsUsingScroll( objectsQuery ) ).hasHitsAnyOrder( mainLoadedObject, emptyLoadedObject );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void callGetProjectionHitMapperEveryTime() {
		DocumentReference mainReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyReference = reference( index.typeName(), EMPTY_ID );

		SearchLoadingContext<DocumentReference, DocumentReference> loadingContextMock =
				mock( SearchLoadingContext.class );
		DocumentReferenceConverter<DocumentReference> documentReferenceConverterMock =
				mock( DocumentReferenceConverter.class );

		GenericStubMappingScope<DocumentReference, DocumentReference> scope = index.createGenericScope();
		SearchQuery<DocumentReference> query = scope.query( loadingContextMock )
				.selectEntity()
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainReference )
						.load( emptyReference, emptyReference )
		);
		query.fetchAll();
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// Second query execution to make sure the backend doesn't try to cache the projection hit mapper...
		reset( loadingContextMock );
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( mainReference, mainReference )
						.load( emptyReference, emptyReference )
		);
		query.fetchAll();
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void failedEntityLoading_skipHit() {
		DocumentReference mainDocumentReference = reference( index.typeName(), MAIN_ID );
		DocumentReference emptyDocumentReference = reference( index.typeName(), EMPTY_ID );
		StubLoadedObject emptyLoadedObject = new StubLoadedObject( emptyDocumentReference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						// Return "null" when loading, meaning the entity failed to load
						.load( mainDocumentReference, null )
						.load( emptyDocumentReference, emptyLoadedObject )
		);
		// Expect the main document to be excluded from hits, since it could not be loaded.
		assertThatQuery( objectsQuery ).hasHitsAnyOrder( emptyLoadedObject );
	}

	private static <H> List<H> hitsUsingScroll(SearchQuery<H> query) {
		try ( SearchScroll<H> scroll = query.scroll( 10 ) ) {
			return scroll.next().hits();
		}
	}

	private void initData() {
		index.bulkIndexer()
				.add( MAIN_ID, document -> {
					document.addValue( index.binding().string, STRING_VALUE );
					document.addValue( index.binding().string_analyzed, STRING_ANALYZED_VALUE );
					document.addValue( index.binding().integer, INTEGER_VALUE );
					document.addValue( index.binding().localDate, LOCAL_DATE_VALUE );
					document.addValue( index.binding().geoPoint, GEO_POINT_VALUE );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( index.binding().flattenedObject.self );
					flattenedObject.addValue( index.binding().flattenedObject.string, FLATTENED_OBJECT_STRING_VALUE );
					flattenedObject.addValue( index.binding().flattenedObject.integer, FLATTENED_OBJECT_INTEGER_VALUE );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( index.binding().nestedObject.self );
					nestedObject.addValue( index.binding().nestedObject.string, NESTED_OBJECT_STRING_VALUE );
					nestedObject.addValue( index.binding().nestedObject.integer, NESTED_OBJECT_INTEGER_VALUE );
				} )
				.add( EMPTY_ID, document -> { } )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<LocalDate> localDate;
		final IndexFieldReference<GeoPoint> geoPoint;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
			string_analyzed = root.field(
					"string_analyzed",
					f -> f.asString()
							.projectable( Projectable.YES )
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
			localDate = root.field(
					"localDate",
					f -> f.asLocalDate().projectable( Projectable.YES )
			)
					.toReference();
			geoPoint = root.field(
					"geoPoint",
					f -> f.asGeoPoint().projectable( Projectable.YES )
			)
					.toReference();
			IndexSchemaObjectField flattenedObjectField =
					root.objectField( "flattenedObject", ObjectStructure.FLATTENED );
			flattenedObject = new ObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectStructure.NESTED );
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
			integer = objectField.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
		}
	}

}
