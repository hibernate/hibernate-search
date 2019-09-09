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

import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubScopeModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.StubSearchSortBuilderFactory;

class StubIndexScope implements IndexScope<StubQueryElementCollector> {
	private final StubSearchPredicateBuilderFactory predicateFactory;
	private final StubSearchSortBuilderFactory sortFactory;
	private final StubSearchQueryBuilderFactory queryFactory;
	private final StubSearchProjectionBuilderFactory projectionFactory;

	private StubIndexScope(Builder builder) {
		Set<String> immutableIndexNames = Collections.unmodifiableSet( new LinkedHashSet<>( builder.indexNames ) );
		Set<StubIndexSchemaNode> immutableRootSchemaNodes =
				Collections.unmodifiableSet( new LinkedHashSet<>( builder.rootSchemaNodes ) );
		StubScopeModel model = new StubScopeModel( immutableIndexNames, immutableRootSchemaNodes );
		this.predicateFactory = new StubSearchPredicateBuilderFactory();
		this.sortFactory = new StubSearchSortBuilderFactory();
		this.projectionFactory = new StubSearchProjectionBuilderFactory( model );
		this.queryFactory = new StubSearchQueryBuilderFactory( builder.backend, model );
	}

	@Override
	public StubSearchPredicateBuilderFactory getSearchPredicateBuilderFactory() {
		return predicateFactory;
	}

	@Override
	public StubSearchSortBuilderFactory getSearchSortBuilderFactory() {
		return sortFactory;
	}

	@Override
	public StubSearchQueryBuilderFactory getSearchQueryBuilderFactory() {
		return queryFactory;
	}

	@Override
	public SearchProjectionBuilderFactory getSearchProjectionFactory() {
		return projectionFactory;
	}

	@Override
	public SearchAggregationBuilderFactory<? super StubQueryElementCollector> getSearchAggregationFactory() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	static class Builder implements IndexScopeBuilder {

		private final StubBackend backend;
		// In a real mapper, this should be used for some features; keeping this here in case we need to stub such feature
		@SuppressWarnings("unused")
		private final BackendMappingContext mappingContext;
		private final Set<String> indexNames = new LinkedHashSet<>();
		private final Set<StubIndexSchemaNode> rootSchemaNodes = new LinkedHashSet<>();

		Builder(StubBackend backend, BackendMappingContext mappingContext, String indexName, StubIndexSchemaNode rootSchemaNode) {
			this.backend = backend;
			this.mappingContext = mappingContext;
			this.indexNames.add( indexName );
			this.rootSchemaNodes.add( rootSchemaNode );
		}

		void add(StubBackend backend, String indexName, StubIndexSchemaNode rootSchemaNode) {
			if ( !this.backend.equals( backend ) ) {
				throw new IllegalStateException( "Attempt to build a scope spanning two distinct backends; this is not possible." );
			}
			indexNames.add( indexName );
			rootSchemaNodes.add( rootSchemaNode );
		}

		@Override
		public IndexScope<?> build() {
			return new StubIndexScope( this );
		}
	}
}
