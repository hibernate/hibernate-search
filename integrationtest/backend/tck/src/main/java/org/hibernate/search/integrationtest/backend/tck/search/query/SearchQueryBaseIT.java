/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSearchLoadingContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
	@TestForIssue(jiraKey = "HSEARCH-4183")
	public void tookAndTimedOut() {
		SearchQuery<DocumentReference> query = matchAllSortedByScoreQuery()
				.toQuery();

		SearchResult<DocumentReference> result = query.fetchAll();

		assertThat( result.took() ).isBetween( Duration.ZERO, Duration.of( 1, ChronoUnit.MINUTES ) );
		assertThat( result.timedOut() ).isFalse();
	}

	@Test
	public void resultTotal() {
		initData( 5000 );

		SearchResult<DocumentReference> fetch = matchAllSortedByScoreQuery()
				.fetch( 10 );

		SearchResultTotal resultTotal = fetch.total();
		assertThat( resultTotal.isHitCountExact() ).isTrue();
		assertThat( resultTotal.isHitCountLowerBound() ).isFalse();
		assertThat( resultTotal.hitCount() ).isEqualTo( 5000 );
		assertThat( resultTotal.hitCountLowerBound() ).isEqualTo( 5000 );
	}

	@Test
	public void resultTotal_totalHitCountThreshold() {
		assumeTrue(
				"This backend doesn't take totalHitsThreshold() into account.",
				TckConfiguration.get().getBackendFeatures().supportsTotalHitsThresholdForSearch()
		);

		initData( 5000 );

		SearchResult<DocumentReference> fetch = matchAllWithConditionSortedByScoreQuery()
				.totalHitCountThreshold( 100 )
				.toQuery()
				.fetch( 10 );

		SearchResultTotal resultTotal = fetch.total();
		assertThat( resultTotal.isHitCountExact() ).isFalse();
		assertThat( resultTotal.isHitCountLowerBound() ).isTrue();
		assertThat( resultTotal.hitCountLowerBound() ).isLessThanOrEqualTo( 5000 );

		assertThatThrownBy( () -> resultTotal.hitCount() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to provide the exact total hit count: only a lower-bound approximation is available.",
						"This is generally the result of setting query options such as a timeout or the total hit count threshold",
						"unset these options, or retrieve the lower-bound hit count approximation"
				);
	}

	@Test
	public void resultTotal_totalHitCountThreshold_veryHigh() {
		initData( 5000 );

		SearchResult<DocumentReference> fetch = matchAllWithConditionSortedByScoreQuery()
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

		SearchQuery<DocumentReference> query = matchAllSortedByScoreQuery().toQuery();

		// Mandatory extension, supported
		QueryWrapper<DocumentReference> extendedQuery = query.extension( new SupportedQueryExtension<>() );
		assertThatResult( extendedQuery.extendedFetch() ).fromQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), "0", "1" );

		// Mandatory extension, unsupported
		assertThatThrownBy(
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
		assertThatThrownBy(
				() -> scope.query()
						.extension( new UnSupportedQueryDslExtension<>() )
		)
				.isInstanceOf( SearchException.class );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllSortedByScoreQuery() {
		return index.query()
				.where( f -> f.matchAll() );
	}

	/**
	 * @return A query that matches all documents, but still has a condition (not a MatchAllDocsQuery).
	 * Necessary when we want to test the total hit count with a total hit count threshold,
	 * because optimizations are possible with MatchAllDocsQuery that would allow Hibernate Search
	 * to return an exact total hit count in constant time, ignoring the total hit count threshold.
	 */
	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllWithConditionSortedByScoreQuery() {
		return index.query()
				.where( f -> f.exists().field( "string" ) );
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
				SearchLoadingContext<?, ?> loadingContext) {
			assertThat( original ).isNotNull();
			assertThat( loadingContext ).isNotNull().isInstanceOf( StubSearchLoadingContext.class );
			return Optional.of( new QueryWrapper<>( original ) );
		}
	}

	private static class UnSupportedQueryExtension<H> implements SearchQueryExtension<QueryWrapper<H>, H> {
		@Override
		public Optional<QueryWrapper<H>> extendOptional(SearchQuery<H> original,
				SearchLoadingContext<?, ?> loadingContext) {
			assertThat( original ).isNotNull();
			assertThat( loadingContext ).isNotNull().isInstanceOf( StubSearchLoadingContext.class );
			return Optional.empty();
		}
	}

	private static class SupportedQueryDslExtension<R, E, LOS> implements
			SearchQueryDslExtension<MyExtendedDslContext<R>, R, E, LOS> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQuerySelectStep<?, R, E, LOS, ?, ?> original,
				SearchQueryIndexScope<?> scope, BackendSessionContext sessionContext,
				SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
			assertThat( original ).isNotNull();
			assertThat( scope ).isNotNull();
			assertThat( sessionContext ).isNotNull();
			assertThat( loadingContextBuilder ).isNotNull();
			return Optional.of( new MyExtendedDslContext<R>( original.selectEntityReference() ) );
		}
	}

	private static class UnSupportedQueryDslExtension<R, E, LOS> implements
			SearchQueryDslExtension<MyExtendedDslContext<R>, R, E, LOS> {
		@Override
		public Optional<MyExtendedDslContext<R>> extendOptional(SearchQuerySelectStep<?, R, E, LOS, ?, ?> original,
				SearchQueryIndexScope<?> scope, BackendSessionContext sessionContext,
				SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
			assertThat( original ).isNotNull();
			assertThat( scope ).isNotNull();
			assertThat( sessionContext ).isNotNull();
			assertThat( loadingContextBuilder ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedDslContext<T> {
		private final SearchQueryWhereStep<?, T, ?, ?> delegate;

		MyExtendedDslContext(SearchQueryWhereStep<?, T, ?, ?> delegate) {
			this.delegate = delegate;
		}

		public SearchQuery<T> extendedFeature(String fieldName, String value1, String value2) {
			return delegate.where( f -> f.or(
							f.match().field( fieldName ).matching( value1 ),
							f.match().field( fieldName ).matching( value2 )
					) )
					.sort( f -> f.field( fieldName ) )
					.toQuery();
		}
	}
}
