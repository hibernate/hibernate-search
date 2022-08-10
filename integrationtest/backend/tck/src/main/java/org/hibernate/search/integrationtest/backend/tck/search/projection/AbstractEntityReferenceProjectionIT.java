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
import static org.mockito.Mockito.verify;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings("unchecked") // Mocking parameterized types
public abstract class AbstractEntityReferenceProjectionIT {

	private static final String DOCUMENT_1_ID = "1";
	private static final String DOCUMENT_2_ID = "2";

	private static final ProjectionMappedTypeContext mainTypeContextMock = Mockito.mock( ProjectionMappedTypeContext.class );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final StubMappedIndex mainIndex;

	protected AbstractEntityReferenceProjectionIT(StubMappedIndex mainIndex) {
		this.mainIndex = mainIndex;
	}

	public abstract <R, E, LOS> SearchQueryWhereStep<?, R, LOS, ?> select(
			SearchQuerySelectStep<?, R, E, LOS, ?, ?> step);

	@Test
	public void noReferenceTransformer() {
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
	public void referenceTransformer() {
		DocumentReference doc1Reference = reference( mainIndex.typeName(), DOCUMENT_1_ID );
		DocumentReference doc2Reference = reference( mainIndex.typeName(), DOCUMENT_2_ID );
		StubTransformedReference doc1TransformedReference = new StubTransformedReference( doc1Reference );
		StubTransformedReference doc2TransformedReference = new StubTransformedReference( doc2Reference );

		SearchLoadingContext<StubTransformedReference, StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		mainIndex.mapping().with()
				.typeContext( mainIndex.typeName(), mainTypeContextMock )
				.run( () -> {
					GenericStubMappingScope<StubTransformedReference, StubEntity> scope =
							mainIndex.createGenericScope( loadingContextMock );
					SearchQuery<StubTransformedReference> referencesQuery = select( scope.query() )
							.where( f -> f.matchAll() )
							.toQuery();

					expectHitMapping(
							loadingContextMock,
							c -> c
									.entityReference( doc1Reference, doc1TransformedReference )
									.entityReference( doc2Reference, doc2TransformedReference )
					);
					assertThatQuery( referencesQuery ).hasHitsAnyOrder( doc1TransformedReference, doc2TransformedReference );
					// Check in particular that the backend gets the projection hit mapper from the loading context,
					// which must happen every time we execute the query,
					// so that the mapper can run state checks (session is still open, ...).
					verify( loadingContextMock ).createProjectionHitMapper();

					// check the same for the scroll API
					expectHitMapping(
							loadingContextMock,
							c -> c
									.entityReference( doc1Reference, doc1TransformedReference )
									.entityReference( doc2Reference, doc2TransformedReference )
					);
					assertThatHits( hitsUsingScroll( referencesQuery ) ).hasHitsAnyOrder( doc1TransformedReference, doc2TransformedReference );
					verify( loadingContextMock ).createProjectionHitMapper();
				} );
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
