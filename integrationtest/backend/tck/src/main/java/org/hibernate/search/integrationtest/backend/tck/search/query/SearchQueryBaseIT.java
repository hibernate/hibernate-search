/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubLoadingContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;
	private IndexMapping indexMapping;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void getQueryString() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.match().onField( "string" ).matching( "platypus" ) )
				.toQuery();

		assertThat( query.getQueryString() ).contains( "platypus" );
	}

	@Test
	public void extension() {
		initData( 2 );

		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		// Mandatory extension, supported
		QueryWrapper<DocumentReference> extendedQuery = query.extension( new SupportedQueryExtension<>() );
		SearchResultAssert.assertThat( extendedQuery.extendedFetch() ).fromQuery( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, "0","1" );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> query.extension( new UnSupportedQueryExtension<>() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void context_extension() {
		initData( 5 );

		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = scope.query()
				.extension( new SupportedQueryDslExtension<>() )
				.extendedFeature( "string", "value1", "value2" );
		SearchResultAssert.assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, "1","2" );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> scope.query()
				.extension( new UnSupportedQueryDslExtension<>() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	private void initData(int documentCount) {
		IndexDocumentWorkExecutor<? extends DocumentElement> executor =
				indexManager.createDocumentWorkExecutor( DocumentCommitStrategy.NONE );
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for ( int i = 0; i < documentCount; i++ ) {
			int intValue = i;
			futures.add( executor.add( referenceProvider( String.valueOf( intValue ) ), document -> {
				document.addValue( indexMapping.string, "value" + intValue );
			} ) );
		}

		CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) ).join();
		indexManager.createWorkExecutor().flush().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		SearchResultAssert.assertThat( query ).hasTotalHitCount( documentCount );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class QueryWrapper<H> {
		private final SearchQuery<H> query;

		private QueryWrapper(SearchQuery<H> query) {
			this.query = query;
		}

		public SearchResult<H> extendedFetch() {
			return query.fetch();
		}
	}

	private static class SupportedQueryExtension<H> implements SearchQueryExtension<QueryWrapper<H>, H> {
		@Override
		public Optional<QueryWrapper<H>> extendOptional(SearchQuery<H> original,
				LoadingContext<?, ?> loadingContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( loadingContext ).isNotNull().isInstanceOf( StubLoadingContext.class );
			return Optional.of( new QueryWrapper<>( original ) );
		}
	}

	private static class UnSupportedQueryExtension<H> implements SearchQueryExtension<QueryWrapper<H>, H> {
		@Override
		public Optional<QueryWrapper<H>> extendOptional(SearchQuery<H> original,
				LoadingContext<?, ?> loadingContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( loadingContext ).isNotNull().isInstanceOf( StubLoadingContext.class );
			return Optional.empty();
		}
	}

	private static class SupportedQueryDslExtension<R, E> implements SearchQueryContextExtension<MyExtendedDslContext<R>, R, E> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQueryResultDefinitionContext<?, R, E, ?, ?> original,
				IndexSearchScope<?> indexSearchScope, SessionContextImplementor sessionContext,
				LoadingContextBuilder<R, E> loadingContextBuilder) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( indexSearchScope ).isNotNull();
			Assertions.assertThat( sessionContext ).isNotNull();
			Assertions.assertThat( loadingContextBuilder ).isNotNull();
			return Optional.of( new MyExtendedDslContext<R>( original.asReference() ) );
		}
	}

	private static class UnSupportedQueryDslExtension<R, E> implements SearchQueryContextExtension<MyExtendedDslContext<R>, R, E> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQueryResultDefinitionContext<?, R, E, ?, ?> original,
				IndexSearchScope<?> indexSearchScope, SessionContextImplementor sessionContext,
				LoadingContextBuilder<R, E> loadingContextBuilder) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( indexSearchScope ).isNotNull();
			Assertions.assertThat( sessionContext ).isNotNull();
			Assertions.assertThat( loadingContextBuilder ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedDslContext<T> {
		private final SearchQueryResultContext<?, T, ?> delegate;

		MyExtendedDslContext(SearchQueryResultContext<?, T, ?> delegate) {
			this.delegate = delegate;
		}

		public SearchQuery<T> extendedFeature(String fieldName, String value1, String value2) {
			return delegate.predicate( f -> f.bool()
					.should( f.match().onField( fieldName ).matching( value1 ) )
					.should( f.match().onField( fieldName ).matching( value2 ) )
			)
					.sort( f -> f.byField( fieldName ) )
					.toQuery();
		}
	}
}
