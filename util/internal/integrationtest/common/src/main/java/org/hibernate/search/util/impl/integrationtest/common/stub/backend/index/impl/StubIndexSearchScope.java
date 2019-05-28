/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.scope.spi.IndexSearchScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexSearchScope;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchScopeModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.StubSearchSortBuilderFactory;

class StubIndexSearchScope implements IndexSearchScope<StubQueryElementCollector> {
	private final StubSearchPredicateBuilderFactory predicateFactory;
	private final StubSearchSortBuilderFactory sortFactory;
	private final StubSearchQueryBuilderFactory queryFactory;
	private final StubSearchProjectionBuilderFactory projectionFactory;

	private StubIndexSearchScope(Builder builder) {
		List<String> immutableIndexNames = Collections.unmodifiableList( new ArrayList<>( builder.indexNames ) );
		List<StubIndexSchemaNode> immutableRootSchemaNodes =
				Collections.unmodifiableList( new ArrayList<>( builder.rootSchemaNodes ) );
		StubSearchScopeModel model = new StubSearchScopeModel( immutableIndexNames, immutableRootSchemaNodes );
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

	static class Builder implements IndexSearchScopeBuilder {

		private final StubBackend backend;
		// In a real mapper, this should be used for some features; keeping this here in case we need to stub such feature
		@SuppressWarnings("unused")
		private final MappingContextImplementor mappingContext;
		private final List<String> indexNames = new ArrayList<>();
		private final List<StubIndexSchemaNode> rootSchemaNodes = new ArrayList<>();

		Builder(StubBackend backend, MappingContextImplementor mappingContext, String indexName, StubIndexSchemaNode rootSchemaNode) {
			this.backend = backend;
			this.mappingContext = mappingContext;
			this.indexNames.add( indexName );
			this.rootSchemaNodes.add( rootSchemaNode );
		}

		void add(StubBackend backend, String indexName, StubIndexSchemaNode rootSchemaNode) {
			if ( !this.backend.equals( backend ) ) {
				throw new IllegalStateException( "Attempt to run a search query across two distinct backends; this is not possible." );
			}
			indexNames.add( indexName );
			rootSchemaNodes.add( rootSchemaNode );
		}

		@Override
		public IndexSearchScope<?> build() {
			return new StubIndexSearchScope( this );
		}
	}
}
