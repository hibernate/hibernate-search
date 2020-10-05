/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.MapperEasyMockUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubDocumentReferenceConverter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubEntityLoader;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubLoadedObject;
import org.hibernate.search.integrationtest.backend.tck.testsupport.stub.StubTransformedReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.GenericStubMappingScope;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.easymock.EasyMockSupport;

public class SearchQueryScrollResultLoadingIT extends EasyMockSupport {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final IndexItem[] references = new IndexItem[37];

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void resultLoadingOnScrolling() {
		LoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock = createMock( LoadingContext.class );
		DocumentReferenceConverter<StubTransformedReference> documentReferenceConverterMock = createMock( StubDocumentReferenceConverter.class );
		EntityLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock = createMock( StubEntityLoader.class );

		resetAll();
		// No calls expected on the mocks
		replayAll();
		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope = index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ) )
				.toQuery();
		SearchScroll<StubLoadedObject> scroll = objectsQuery.scroll( 5 );
		verifyAll();

		verifyLoading( loadingContextMock, documentReferenceConverterMock, objectLoaderMock, scroll );
	}

	@Test
	public void resultLoadingOnScrolling_entityLoadingTimeout() {
		LoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock = createMock( LoadingContext.class );
		DocumentReferenceConverter<StubTransformedReference> documentReferenceConverterMock = createMock( StubDocumentReferenceConverter.class );
		EntityLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock = createMock( StubEntityLoader.class );

		resetAll();
		// No calls expected on the mocks
		replayAll();
		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope = index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ) )
				.failAfter( 1000, TimeUnit.HOURS )
				.toQuery();
		SearchScroll<StubLoadedObject> scroll = objectsQuery.scroll( 5 );
		verifyAll();

		verifyLoading( loadingContextMock, documentReferenceConverterMock, objectLoaderMock, scroll );
	}

	@Test
	public void resultLoadingOnScrolling_softTimeout() {
		LoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock = createMock( LoadingContext.class );
		DocumentReferenceConverter<StubTransformedReference> documentReferenceConverterMock = createMock( StubDocumentReferenceConverter.class );
		EntityLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock = createMock( StubEntityLoader.class );

		resetAll();
		// No calls expected on the mocks
		replayAll();
		GenericStubMappingScope<StubTransformedReference, StubLoadedObject> scope = index.createGenericScope();
		SearchQuery<StubLoadedObject> objectsQuery = scope.query( loadingContextMock )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ) )
				.truncateAfter( 1000, TimeUnit.HOURS )
				.toQuery();
		SearchScroll<StubLoadedObject> scroll = objectsQuery.scroll( 5 );
		verifyAll();

		// softTimeout is passed to the entity loading too
		verifyLoading( loadingContextMock, documentReferenceConverterMock, objectLoaderMock, scroll );
	}

	private void verifyLoading(LoadingContext<StubTransformedReference, StubLoadedObject> loadingContextMock,
			DocumentReferenceConverter<StubTransformedReference> documentReferenceConverterMock,
			EntityLoader<StubTransformedReference, StubLoadedObject> objectLoaderMock,
			SearchScroll<StubLoadedObject> scroll) {
		// 7 full size pages
		for ( int j = 0; j < 7; j++ ) {
			int base = j * 5;

			resetAll();
			MapperEasyMockUtils.expectHitMapping(
					loadingContextMock, documentReferenceConverterMock, objectLoaderMock,
					c -> {
						for ( int i = 0; i < 5; i++ ) {
							c.load( references[base + i].reference, references[base + i].transformedReference, references[base + i].loadedObject );
						}
					}
			);
			replayAll();
			assertThatHits( scroll.next().hits() ).hasHitsAnyOrder(
					references[base + 0].loadedObject, references[base + 1].loadedObject, references[base + 2].loadedObject,
					references[base + 3].loadedObject, references[base + 4].loadedObject
			);
			verifyAll();
		}

		// page with the rest items
		resetAll();
		MapperEasyMockUtils.expectHitMapping(
				loadingContextMock, documentReferenceConverterMock, objectLoaderMock,
				c -> {
					for ( int i = 35; i <= 36; i++ ) {
						c.load( references[i].reference, references[i].transformedReference, references[i].loadedObject );
					}
				}
		);
		replayAll();
		assertThatHits( scroll.next().hits() ).hasHitsAnyOrder(
				references[35].loadedObject, references[36].loadedObject
		);
		verifyAll();
	}

	private void initData() {
		BulkIndexer indexer = index.bulkIndexer();
		for ( int i = 0; i < 37; i++ ) {
			IndexItem item = new IndexItem( i );
			references[i] = item;
			indexer.add( item.id, document -> document.addValue( "integer", item.value ) );
		}
		indexer.join();
	}

	private class IndexItem {
		final String id;
		final int value;
		final DocumentReference reference;
		final StubTransformedReference transformedReference;
		final StubLoadedObject loadedObject;

		IndexItem(int i) {
			id = i + "";
			value = i;
			reference = reference( index.typeName(), id );
			transformedReference = new StubTransformedReference( reference );
			loadedObject = new StubLoadedObject( reference );
		}
	}

	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
		}
	}
}
