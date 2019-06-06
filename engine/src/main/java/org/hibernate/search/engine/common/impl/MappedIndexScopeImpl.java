/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.impl.DefaultSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortContainerContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

class MappedIndexScopeImpl<C, R, E> implements MappedIndexScope<R, E> {

	private final IndexScope<C> delegate;

	MappedIndexScopeImpl(IndexScope<C> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "delegate=" ).append( delegate )
				.append( "]" )
				.toString();
	}

	@Override
	public SearchQueryResultDefinitionContext<?, R, E, SearchProjectionFactoryContext<R, E>, ?> search(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		return new DefaultSearchQueryResultDefinitionContext<>( delegate, sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new DefaultSearchPredicateFactoryContext<>( delegate.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new DefaultSearchSortContainerContext<>( delegate.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext<R, E> projection() {
		return new DefaultSearchProjectionFactoryContext<>( delegate.getSearchProjectionFactory() );
	}
}
