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
import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBase;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.StubSearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.StubSearchSortBuilderFactory;

class StubIndexSearchTarget extends IndexSearchTargetBase implements SearchTargetContext<StubQueryElementCollector> {
	private final StubSearchPredicateBuilderFactory predicateFactory;
	private final StubSearchSortBuilderFactory sortFactory;
	private final StubSearchQueryBuilderFactory queryFactory;
	private final StubSearchProjectionBuilderFactory projectionFactory;

	private StubIndexSearchTarget(Builder builder) {
		this.predicateFactory = new StubSearchPredicateBuilderFactory();
		this.sortFactory = new StubSearchSortBuilderFactory();
		List<String> immutableIndexNames = Collections.unmodifiableList( new ArrayList<>( builder.indexNames ) );
		this.queryFactory = new StubSearchQueryBuilderFactory( builder.backend, immutableIndexNames );
		this.projectionFactory = new StubSearchProjectionBuilderFactory();
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(SessionContextImplementor context,
			Function<DocumentReference, R> documentReferenceTransformer, ObjectLoader<R, O> objectLoader) {
		return new SearchQueryResultDefinitionContextImpl<>( this, context,
				documentReferenceTransformer, objectLoader );
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
	protected SearchTargetContext<?> getSearchTargetContext() {
		return this;
	}

	static class Builder implements IndexSearchTargetBuilder {

		private final StubBackend backend;
		private final MappingContextImplementor mappingContext;
		private final List<String> indexNames = new ArrayList<>();

		Builder(StubBackend backend, MappingContextImplementor mappingContext, String indexName) {
			this.backend = backend;
			this.mappingContext = mappingContext;
			this.indexNames.add( indexName );
		}

		void add(StubBackend backend, String indexName) {
			if ( !this.backend.equals( backend ) ) {
				throw new IllegalStateException( "Attempt to run a search query across two distinct backends; this is not possible." );
			}
			indexNames.add( indexName );
		}

		@Override
		public IndexSearchTarget build() {
			return new StubIndexSearchTarget( this );
		}
	}
}
