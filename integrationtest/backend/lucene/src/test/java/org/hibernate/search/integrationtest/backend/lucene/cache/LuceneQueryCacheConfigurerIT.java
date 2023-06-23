/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.io.IOException;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurationContext;
import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurer;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Version;

public class LuceneQueryCacheConfigurerIT {

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void error_failingQueryCacheConfigurer() {
		setup( FailingQueryCacheConfigurer.class.getName() );
		initData( 10 );

		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( "platypus" ) )
				.toQuery();

		assertThatThrownBy( () -> query.fetchAll() )
				.isInstanceOf( SimulatedFailure.class );
	}

	public static class FailingQueryCacheConfigurer implements QueryCachingConfigurer {
		private static final String FAILURE_MESSAGE = "Simulated failure for " + FailingQueryCacheConfigurer.class.getName();

		@Override
		public void configure(QueryCachingConfigurationContext context) {
			context.queryCache(
					new TestQueryCache( context.luceneVersion(), new SimulatedFailure( FAILURE_MESSAGE ) ) );
		}
	}

	@Test
	public void error_failingQueryCachingPolicyConfigurer() {
		setup( FailingCachePolicyExceptionQueryCacheConfigurer.class.getName() );
		initData( 10 );

		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( "platypus" ) )
				.toQuery();

		assertThatThrownBy( () -> query.fetchAll() )
				.isInstanceOf( SimulatedFailure.class );
	}

	public static class FailingCachePolicyExceptionQueryCacheConfigurer implements QueryCachingConfigurer {
		private static final String FAILURE_MESSAGE =
				"Simulated failure for " + FailingCachePolicyExceptionQueryCacheConfigurer.class.getName();

		@Override
		public void configure(QueryCachingConfigurationContext context) {
			context.queryCachingPolicy(
					new TestQueryCachingPolicy( context.luceneVersion(), new SimulatedFailure( FAILURE_MESSAGE ) ) );
		}
	}

	private void setup(String queryCacheConfigurer) {
		setup( queryCacheConfigurer, c -> {} );
	}

	private void setup(String queryCacheConfigurer, Consumer<IndexBindingContext> binder) {
		setupHelper.start()
				.expectCustomBeans()
				.withBackendProperty( LuceneBackendSettings.QUERY_CACHING_CONFIGURER, queryCacheConfigurer )
				.withIndex( index )
				.setup();
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return index.query()
				.where( f -> f.matchAll() );
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

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

	static class TestQueryCache implements QueryCache {

		private final SimulatedFailure failure;

		TestQueryCache(Version luceneVersion, SimulatedFailure failure) {
			this.failure = failure;
		}

		@Override
		public Weight doCache(Weight weight, QueryCachingPolicy policy) {
			throw failure;
		}

	}

	static class TestQueryCachingPolicy implements QueryCachingPolicy {

		private final SimulatedFailure failure;

		TestQueryCachingPolicy(Version luceneVersion, SimulatedFailure failure) {
			this.failure = failure;
		}

		@Override
		public void onUse(Query query) {
			throw failure;
		}

		@Override
		public boolean shouldCache(Query query) throws IOException {
			return true;
		}
	}

}
