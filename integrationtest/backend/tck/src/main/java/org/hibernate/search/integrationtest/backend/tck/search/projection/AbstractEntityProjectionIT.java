/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.stub.MapperMockUtils.expectHitMapping;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings("unchecked") // Mocking parameterized types
public abstract class AbstractEntityProjectionIT {

	private static final String DOCUMENT_1_ID = "1";
	private static final String DOCUMENT_2_ID = "2";

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final StubMappedIndex mainIndex;

	protected AbstractEntityProjectionIT(StubMappedIndex mainIndex) {
		this.mainIndex = mainIndex;
	}

	public abstract <R, E, LOS> SearchQueryWhereStep<?, E, LOS, ?> select(
			SearchQuerySelectStep<?, R, E, LOS, ?, ?> step);

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3578")
	public void entityLoading() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubLoadedObject doc1LoadedObject = new StubLoadedObject( doc1Reference );
		StubLoadedObject doc2LoadedObject = new StubLoadedObject( doc2Reference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				mainIndex.createGenericScope( loadingContextMock );
		SearchQuery<StubLoadedObject> query = select( scope.query() )
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( doc1Reference, doc1LoadedObject )
						.load( doc2Reference, doc2LoadedObject )
		);
		assertThatQuery( query ).hasHitsAnyOrder( doc1LoadedObject, doc2LoadedObject );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( doc1Reference, doc1LoadedObject )
						.load( doc2Reference, doc2LoadedObject )
		);
		assertThatHits( hitsUsingScroll( query ) ).hasHitsAnyOrder( doc1LoadedObject, doc2LoadedObject );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void entityLoading_timeout() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubLoadedObject doc1LoadedObject = new StubLoadedObject( doc1Reference );
		StubLoadedObject doc2LoadedObject = new StubLoadedObject( doc2Reference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				mainIndex.createGenericScope( loadingContextMock );
		SearchQuery<StubLoadedObject> query = select( scope.query() )
				.where( f -> f.matchAll() )
				.failAfter( 1000L, TimeUnit.HOURS )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( doc1Reference, doc1LoadedObject )
						.load( doc2Reference, doc2LoadedObject )
		);
		assertThatQuery( query ).hasHitsAnyOrder( doc1LoadedObject, doc2LoadedObject );
		// Check in particular that the backend gets the projection hit mapper from the loading context,
		// which must happen every time we execute the query,
		// so that the mapper can run state checks (session is still open, ...).
		verify( loadingContextMock ).createProjectionHitMapper();

		// check the same for the scroll API
		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( doc1Reference, doc1LoadedObject )
						.load( doc2Reference, doc2LoadedObject )
		);
		assertThatHits( hitsUsingScroll( query ) ).hasHitsAnyOrder( doc1LoadedObject, doc2LoadedObject );
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	public void noEntityLoading() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = select( scope.query() )
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1_ID, DOCUMENT_2_ID );

		// check the same for the scroll API
		assertThatHits( hitsUsingScroll( query ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1_ID, DOCUMENT_2_ID );
	}

	@Test
	public void entityLoading_callGetProjectionHitMapperEveryTime() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );

		SearchLoadingContext<DocumentReference, DocumentReference> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<DocumentReference, DocumentReference> scope =
				mainIndex.createGenericScope( loadingContextMock );
		SearchQuery<DocumentReference> query = select( scope.query() )
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						.load( doc1Reference, doc1Reference )
						.load( doc2Reference, doc2Reference )
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
						.load( doc1Reference, doc1Reference )
						.load( doc2Reference, doc2Reference )
		);
		query.fetchAll();
		verify( loadingContextMock ).createProjectionHitMapper();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void entityLoading_failed_skipHit() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubLoadedObject doc2LoadedObject = new StubLoadedObject( doc2Reference );

		SearchLoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock =
				mock( SearchLoadingContext.class );

		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope =
				mainIndex.createGenericScope( loadingContextMock );
		SearchQuery<StubLoadedObject> objectsQuery = select( scope.query() )
				.where( f -> f.matchAll() )
				.toQuery();

		expectHitMapping(
				loadingContextMock,
				c -> c
						// Return "null" when loading, meaning the entity failed to load
						.load( doc1Reference, null )
						.load( doc2Reference, doc2LoadedObject )
		);
		// Expect the main document to be excluded from hits, since it could not be loaded.
		assertThatQuery( objectsQuery ).hasHitsAnyOrder( doc2LoadedObject );
	}

	private static <H> List<H> hitsUsingScroll(SearchQuery<H> query) {
		try ( SearchScroll<H> scroll = query.scroll( 10 ) ) {
			return scroll.next().hits();
		}
	}

	public static void initData(BulkIndexer indexer) {
		indexer
				.add( DOCUMENT_1_ID, document -> { } )
				.add( DOCUMENT_2_ID, document -> { } );
	}

}
