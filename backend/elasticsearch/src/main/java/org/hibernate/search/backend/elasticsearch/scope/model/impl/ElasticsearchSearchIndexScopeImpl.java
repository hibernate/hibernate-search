/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.impl.ElasticsearchSearchAggregationFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighterFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl.ElasticsearchSearchPredicateFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.impl.ElasticsearchSearchProjectionFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.impl.ElasticsearchSearchSortFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class ElasticsearchSearchIndexScopeImpl
		extends AbstractSearchIndexScope<
				ElasticsearchSearchIndexScopeImpl,
				ElasticsearchIndexModel,
				ElasticsearchSearchIndexNodeContext,
				ElasticsearchSearchIndexCompositeNodeContext>
		implements ElasticsearchSearchIndexScope<ElasticsearchSearchIndexScopeImpl>,
		ElasticsearchSearchQueryIndexScope<ElasticsearchSearchIndexScopeImpl> {

	// Backend context
	private final SearchBackendContext backendContext;
	private final Gson userFacingGson;
	private final ElasticsearchSearchSyntax searchSyntax;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final TimingSource timingSource;

	// Targeted indexes
	private final Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex;
	private final int maxResultWindow;

	// Query support
	private final ElasticsearchSearchPredicateBuilderFactory predicateBuilderFactory;
	private final ElasticsearchSearchSortBuilderFactory sortBuilderFactory;
	private final ElasticsearchSearchProjectionBuilderFactory projectionBuilderFactory;
	private final ElasticsearchSearchAggregationBuilderFactory aggregationFactory;

	public ElasticsearchSearchIndexScopeImpl(BackendMappingContext mappingContext,
			SearchBackendContext backendContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			Set<ElasticsearchIndexModel> indexModels) {
		super( mappingContext, indexModels );
		this.backendContext = backendContext;
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;

		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		for ( ElasticsearchIndexModel model : indexModels ) {
			mappedTypeNameToIndex.put( model.mappedTypeName(), model );
		}

		int currentMaxResultWindow = Integer.MAX_VALUE;
		for ( ElasticsearchIndexModel index : indexModels ) {
			if ( index.maxResultWindow() < currentMaxResultWindow ) {
				// take the minimum
				currentMaxResultWindow = index.maxResultWindow();
			}
		}
		this.maxResultWindow = currentMaxResultWindow;

		this.predicateBuilderFactory = new ElasticsearchSearchPredicateBuilderFactory( this );
		this.sortBuilderFactory = new ElasticsearchSearchSortBuilderFactory( this );
		this.projectionBuilderFactory = new ElasticsearchSearchProjectionBuilderFactory(
				backendContext.getSearchProjectionBackendContext(), this );
		this.aggregationFactory = new ElasticsearchSearchAggregationBuilderFactory( this );
	}

	private ElasticsearchSearchIndexScopeImpl(ElasticsearchSearchIndexScopeImpl parentScope,
			ElasticsearchSearchIndexCompositeNodeContext overriddenRoot) {
		super( parentScope, overriddenRoot );
		this.backendContext = parentScope.backendContext;
		this.userFacingGson = parentScope.userFacingGson;
		this.searchSyntax = parentScope.searchSyntax;
		this.multiTenancyStrategy = parentScope.multiTenancyStrategy;
		this.timingSource = parentScope.timingSource;
		this.mappedTypeNameToIndex = parentScope.mappedTypeNameToIndex;
		this.maxResultWindow = parentScope.maxResultWindow;

		this.predicateBuilderFactory = new ElasticsearchSearchPredicateBuilderFactory( this );
		this.sortBuilderFactory = new ElasticsearchSearchSortBuilderFactory( this );
		this.projectionBuilderFactory = new ElasticsearchSearchProjectionBuilderFactory(
				backendContext.getSearchProjectionBackendContext(), this );
		this.aggregationFactory = new ElasticsearchSearchAggregationBuilderFactory( this );
	}

	@Override
	protected ElasticsearchSearchIndexScopeImpl self() {
		return this;
	}

	@Override
	public ElasticsearchSearchIndexScopeImpl withRoot(String objectFieldPath) {
		return new ElasticsearchSearchIndexScopeImpl( this, field( objectFieldPath ).toComposite() );
	}

	@Override
	public ElasticsearchSearchPredicateBuilderFactory predicateBuilders() {
		return predicateBuilderFactory;
	}

	@Override
	public ElasticsearchSearchSortBuilderFactory sortBuilders() {
		return sortBuilderFactory;
	}

	@Override
	public ElasticsearchSearchProjectionBuilderFactory projectionBuilders() {
		return projectionBuilderFactory;
	}

	@Override
	public ElasticsearchSearchAggregationBuilderFactory aggregationBuilders() {
		return aggregationFactory;
	}

	@Override
	public <P> ElasticsearchSearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder, SearchProjection<P> projection) {
		return backendContext.createSearchQueryBuilder( this, sessionContext, loadingContextBuilder,
				ElasticsearchSearchProjection.from( this, projection ) );
	}

	@Override
	public ElasticsearchSearchPredicateFactory predicateFactory() {
		return new ElasticsearchSearchPredicateFactoryImpl( SearchPredicateDslContext.root( this ) );
	}

	@Override
	public ElasticsearchSearchSortFactory sortFactory() {
		return new ElasticsearchSearchSortFactoryImpl( SearchSortDslContext
				.root( this, ElasticsearchSearchSortFactoryImpl::new, predicateFactory() ) );
	}

	@Override
	public <R, E> ElasticsearchSearchProjectionFactory<R, E> projectionFactory() {
		return new ElasticsearchSearchProjectionFactoryImpl<>( SearchProjectionDslContext.root( this ) );
	}

	@Override
	public ElasticsearchSearchAggregationFactory aggregationFactory() {
		return new ElasticsearchSearchAggregationFactoryImpl( SearchAggregationDslContext.root( this, predicateFactory() ) );
	}

	@Override
	public SearchHighlighterFactory highlighterFactory() {
		return new ElasticsearchSearchHighlighterFactory( this );
	}

	@Override
	public ElasticsearchSearchIndexNodeContext field(String fieldPath) {
		return super.field( fieldPath );
	}

	@Override
	public Gson userFacingGson() {
		return userFacingGson;
	}

	@Override
	public ElasticsearchSearchSyntax searchSyntax() {
		return searchSyntax;
	}

	@Override
	public DocumentIdHelper documentIdHelper() {
		return multiTenancyStrategy.documentIdHelper();
	}

	@Override
	public JsonObject filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}

	@Override
	public TimeoutManager createTimeoutManager(Long timeout,
			TimeUnit timeUnit, boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return TimeoutManager.hardTimeout( timingSource, timeout, timeUnit );
			}
			else {
				return TimeoutManager.softTimeout( timingSource, timeout, timeUnit );
			}
		}
		return TimeoutManager.noTimeout( timingSource );
	}

	@Override
	public Collection<ElasticsearchSearchIndexContext> indexes() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public int maxResultWindow() {
		return maxResultWindow;
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeContext createMultiIndexSearchRootContext(
			List<ElasticsearchSearchIndexCompositeNodeContext> rootForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexCompositeNodeContext( this, null,
				rootForEachIndex );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ElasticsearchSearchIndexNodeContext createMultiIndexSearchValueFieldContext(String absolutePath,
			List<ElasticsearchSearchIndexNodeContext> fieldForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexValueFieldContext<>( this, absolutePath,
				(List) fieldForEachIndex );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ElasticsearchSearchIndexNodeContext createMultiIndexSearchObjectFieldContext(String absolutePath,
			List<ElasticsearchSearchIndexNodeContext> fieldForEachIndex) {
		return new ElasticsearchMultiIndexSearchIndexCompositeNodeContext( this, absolutePath,
				(List) fieldForEachIndex );
	}

}
