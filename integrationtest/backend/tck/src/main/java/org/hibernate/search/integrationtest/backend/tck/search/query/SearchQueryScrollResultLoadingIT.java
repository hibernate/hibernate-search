/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.stub.MapperMockUtils.expectHitMapping;
import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class SearchQueryScrollResultLoadingIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final ProjectionMappedTypeContext typeContextMock = Mockito.mock( ProjectionMappedTypeContext.class );

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final IndexItem[] references = new IndexItem[37];

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( int i = 0; i < 37; i++ ) {
			IndexItem item = new IndexItem( i );
			references[i] = item;
			indexer.add( item.id, document -> document.addValue( "integer", item.value ) );
		}
		indexer.join();
	}

	@Test
	public void resultLoadingOnScrolling() {
		@SuppressWarnings("unchecked")
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope = index.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = scope.query()
							.where( f -> f.matchAll() )
							.sort( f -> f.field( "integer" ) )
							.toQuery();
					SearchScroll<StubEntity> scroll = query.scroll( 5 );

					verifyLoading( loadingContextMock, scroll );
				} );
	}

	@Test
	public void resultLoadingOnScrolling_entityLoadingTimeout() {
		@SuppressWarnings("unchecked")
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = scope.query()
							.where( f -> f.matchAll() )
							.sort( f -> f.field( "integer" ) )
							.failAfter( 1000, TimeUnit.HOURS )
							.toQuery();
					SearchScroll<StubEntity> scroll = query.scroll( 5 );

					verifyLoading( loadingContextMock, scroll );
				} );
	}

	@Test
	public void resultLoadingOnScrolling_softTimeout() {
		@SuppressWarnings("unchecked")
		SearchLoadingContext<StubEntity> loadingContextMock =
				mock( SearchLoadingContext.class );

		when( typeContextMock.loadingAvailable() ).thenReturn( true );

		index.mapping().with()
				.typeContext( index.typeName(), typeContextMock )
				.run( () -> {
					GenericStubMappingScope<EntityReference, StubEntity> scope =
							index.createGenericScope( loadingContextMock );
					SearchQuery<StubEntity> query = scope.query()
							.where( f -> f.matchAll() )
							.sort( f -> f.field( "integer" ) )
							.truncateAfter( 1000, TimeUnit.HOURS )
							.toQuery();
					SearchScroll<StubEntity> scroll = query.scroll( 5 );

					// softTimeout is passed to the entity loading too
					verifyLoading( loadingContextMock, scroll );
				} );
	}

	private void verifyLoading(SearchLoadingContext<StubEntity> loadingContextMock,
			SearchScroll<StubEntity> scroll) {
		// 7 full size pages
		for ( int j = 0; j < 7; j++ ) {
			int base = j * 5;

			expectHitMapping(
					loadingContextMock,
					c -> {
						for ( int i = 0; i < 5; i++ ) {
							c.load( references[base + i].reference, references[base + i].loadedEntity );
						}
					}
			);
			SearchScrollResult<StubEntity> chunk = scroll.next();
			assertThatHits( chunk.hits() ).hasHitsAnyOrder(
					references[base + 0].loadedEntity, references[base + 1].loadedEntity, references[base + 2].loadedEntity,
					references[base + 3].loadedEntity, references[base + 4].loadedEntity
			);
			// Check in particular that the backend gets the projection hit mapper from the loading context,
			// which must happen every time we load entities,
			// so that the mapper can run state checks (session is still open, ...).
			verify( loadingContextMock ).createProjectionHitMapper();
			assertThat( chunk.total().hitCount() ).isEqualTo( 37 );
		}

		// page with the few remaining items
		expectHitMapping(
				loadingContextMock,
				c -> {
					for ( int i = 35; i <= 36; i++ ) {
						c.load( references[i].reference, references[i].loadedEntity );
					}
				}
		);
		SearchScrollResult<StubEntity> chunk = scroll.next();
		assertThatHits( chunk.hits() ).hasHitsAnyOrder(
				references[35].loadedEntity, references[36].loadedEntity
		);
		verify( loadingContextMock ).createProjectionHitMapper();
		assertThat( chunk.total().hitCount() ).isEqualTo( 37 );
	}

	private static class IndexItem {
		final String id;
		final int value;
		final DocumentReference reference;
		final StubEntity loadedEntity;

		IndexItem(int i) {
			id = i + "";
			value = i;
			reference = reference( index.typeName(), id );
			loadedEntity = new StubEntity( reference );
		}
	}

	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
		}
	}
}
