/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Optional;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryBaseIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void getQueryString() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( "platypus" ) )
				.toQuery();

		assertThat( query.queryString() ).contains( "platypus" );
	}

	@Test
	public void tookAndTimedOut() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		SearchResult<DocumentReference> result = query.fetchAll();

		assertNotNull( result.took() );
		assertNotNull( result.timedOut() );
		assertFalse( result.timedOut() );
	}

	@Test
	public void resultTotal() {
		initData( 5000 );
		StubMappingScope scope = index.createScope();

		SearchResult<DocumentReference> fetch = scope.query()
				.where( f -> f.matchAll() )
				.toQuery()
				.fetch( 10 );

		SearchResultTotal resultTotal = fetch.total();
		assertThat( resultTotal.isHitCountExact() ).isTrue();
		assertThat( resultTotal.isHitCountLowerBound() ).isFalse();
		assertThat( resultTotal.hitCount() ).isEqualTo( 5000 );
		assertThat( resultTotal.hitCountLowerBound() ).isEqualTo( 5000 );
	}

	@Test
	@SuppressWarnings("deprecation") // we can remove the test when the deprecated API is removed
	public void totalHitCount_deprecated() {
		initData( 5000 );
		StubMappingScope scope = index.createScope();

		SearchResult<DocumentReference> fetch = scope.query()
				.where( f -> f.matchAll() )
				.toQuery()
				.fetch( 10 );

		assertThat( fetch.totalHitCount() ).isEqualTo( 5000 );
	}

	@Test
	public void resultTotal_totalHitCountThreshold() {
		assumeTrue(
				"This backend doesn't take totalHitsThreshold() into account.",
				TckConfiguration.get().getBackendFeatures().supportsTotalHitsThreshold()
		);

		initData( 5000 );
		StubMappingScope scope = index.createScope();

		SearchResult<DocumentReference> fetch = scope.query()
				.where( f -> f.matchAll() )
				.totalHitCountThreshold( 100 )
				.toQuery()
				.fetch( 10 );

		SearchResultTotal resultTotal = fetch.total();
		assertThat( resultTotal.isHitCountExact() ).isFalse();
		assertThat( resultTotal.isHitCountLowerBound() ).isTrue();
		assertThat( resultTotal.hitCountLowerBound() ).isLessThanOrEqualTo( 5000 );

		Assertions.assertThatThrownBy( () -> resultTotal.hitCount() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Trying to get the exact total hit count, but it is a lower bound." );
	}

	@Test
	public void resultTotal_totalHitCountThreshold_veryHigh() {
		initData( 5000 );
		StubMappingScope scope = index.createScope();

		SearchResult<DocumentReference> fetch = scope.query()
				.where( f -> f.matchAll() )
				.totalHitCountThreshold( 5000 )
				.toQuery()
				.fetch( 10 );

		SearchResultTotal resultTotal = fetch.total();
		assertThat( resultTotal.isHitCountExact() ).isTrue();
		assertThat( resultTotal.isHitCountLowerBound() ).isFalse();
		assertThat( resultTotal.hitCount() ).isEqualTo( 5000 );
		assertThat( resultTotal.hitCountLowerBound() ).isEqualTo( 5000 );
	}

	@Test
	public void extension() {
		initData( 2 );

		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Mandatory extension, supported
		QueryWrapper<DocumentReference> extendedQuery = query.extension( new SupportedQueryExtension<>() );
		assertThatResult( extendedQuery.extendedFetch() ).fromQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "0", "1" );

		// Mandatory extension, unsupported
		Assertions.assertThatThrownBy(
				() -> query.extension( new UnSupportedQueryExtension<>() )
		)
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void context_extension() {
		initData( 5 );

		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = scope.query()
				.extension( new SupportedQueryDslExtension<>() )
				.extendedFeature( "string", "value1", "value2" );
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( index.typeName(), "1", "2" );

		// Mandatory extension, unsupported
		Assertions.assertThatThrownBy(
				() -> scope.query()
						.extension( new UnSupportedQueryDslExtension<>() )
		)
				.isInstanceOf( SearchException.class );
	}

	private void initData(int documentCount) {
		index.bulkIndexer()
				.add( documentCount, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().string, "value" + i )
				) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
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
			return query.fetchAll();
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

	private static class SupportedQueryDslExtension<R, E, LOS> implements
			SearchQueryDslExtension<MyExtendedDslContext<R>, R, E, LOS> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQuerySelectStep<?, R, E, LOS, ?, ?> original,
				IndexScope<?> indexScope, BackendSessionContext sessionContext,
				LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( indexScope ).isNotNull();
			Assertions.assertThat( sessionContext ).isNotNull();
			Assertions.assertThat( loadingContextBuilder ).isNotNull();
			return Optional.of( new MyExtendedDslContext<R>( original.selectEntityReference() ) );
		}
	}

	private static class UnSupportedQueryDslExtension<R, E, LOS> implements
			SearchQueryDslExtension<MyExtendedDslContext<R>, R, E, LOS> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQuerySelectStep<?, R, E, LOS, ?, ?> original,
				IndexScope<?> indexScope, BackendSessionContext sessionContext,
				LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( indexScope ).isNotNull();
			Assertions.assertThat( sessionContext ).isNotNull();
			Assertions.assertThat( loadingContextBuilder ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedDslContext<T> {
		private final SearchQueryWhereStep<?, T, ?, ?> delegate;

		MyExtendedDslContext(SearchQueryWhereStep<?, T, ?, ?> delegate) {
			this.delegate = delegate;
		}

		public SearchQuery<T> extendedFeature(String fieldName, String value1, String value2) {
			return delegate.where( f -> f.bool()
					.should( f.match().field( fieldName ).matching( value1 ) )
					.should( f.match().field( fieldName ).matching( value2 ) )
			)
					.sort( f -> f.field( fieldName ) )
					.toQuery();
		}
	}
}
