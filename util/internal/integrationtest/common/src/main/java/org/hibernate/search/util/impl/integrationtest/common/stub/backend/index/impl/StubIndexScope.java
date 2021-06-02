/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregationBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSortBuilderFactory;

public class StubIndexScope implements IndexScope<StubQueryElementCollector> {
	private final StubSearchPredicateBuilderFactory predicateFactory;
	private final StubSearchSortBuilderFactory sortFactory;
	private final StubSearchProjectionBuilderFactory projectionFactory;
	private final StubSearchAggregationBuilderFactory aggregationFactory;
	private final StubSearchQueryBuilderFactory queryFactory;

	private StubIndexScope(Builder builder) {
		Set<StubIndexModel> immutableIndexModels =
				Collections.unmodifiableSet( new LinkedHashSet<>( builder.indexModels ) );
		StubSearchIndexScope scope = new StubSearchIndexScope( immutableIndexModels );
		this.predicateFactory = new StubSearchPredicateBuilderFactory( scope );
		this.sortFactory = new StubSearchSortBuilderFactory( scope );
		this.projectionFactory = new StubSearchProjectionBuilderFactory( scope );
		this.aggregationFactory = new StubSearchAggregationBuilderFactory( scope );
		this.queryFactory = new StubSearchQueryBuilderFactory( builder.backend, scope );
	}

	@Override
	public StubSearchPredicateBuilderFactory searchPredicateBuilderFactory() {
		return predicateFactory;
	}

	@Override
	public StubSearchSortBuilderFactory searchSortBuilderFactory() {
		return sortFactory;
	}

	@Override
	public StubSearchQueryBuilderFactory searchQueryBuilderFactory() {
		return queryFactory;
	}

	@Override
	public SearchProjectionBuilderFactory searchProjectionFactory() {
		return projectionFactory;
	}

	@Override
	public SearchAggregationBuilderFactory<? super StubQueryElementCollector> searchAggregationFactory() {
		return aggregationFactory;
	}

	static class Builder implements IndexScopeBuilder {

		private final StubBackend backend;
		// In a real mapper, this should be used for some features; keeping this here in case we need to stub such feature
		@SuppressWarnings("unused")
		private final BackendMappingContext mappingContext;
		private final Set<StubIndexModel> indexModels = new LinkedHashSet<>();

		Builder(StubBackend backend, BackendMappingContext mappingContext, StubIndexModel model) {
			this.backend = backend;
			this.mappingContext = mappingContext;
			this.indexModels.add( model );
		}

		void add(StubBackend backend, StubIndexModel model) {
			if ( !this.backend.equals( backend ) ) {
				throw new IllegalStateException( "Attempt to build a scope spanning two distinct backends; this is not possible." );
			}
			indexModels.add( model );
		}

		@Override
		public IndexScope<?> build() {
			return new StubIndexScope( this );
		}
	}
}
