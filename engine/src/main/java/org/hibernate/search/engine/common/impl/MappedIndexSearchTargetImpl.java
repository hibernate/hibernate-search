/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.SearchProjectionFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.query.impl.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchTargetSortRootContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

class MappedIndexSearchTargetImpl implements MappedIndexSearchTarget {

	private final SearchTargetContext<?> searchTargetContext;

	MappedIndexSearchTargetImpl(SearchTargetContext<?> searchTargetContext) {
		this.searchTargetContext = searchTargetContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "context=" ).append( searchTargetContext )
				.append( "]" )
				.toString();
	}

	@Override
	public SearchQueryResultDefinitionContext<DocumentReference, DocumentReference> query(
			SessionContextImplementor context) {
		return query( context, Function.identity(), ObjectLoader.identity() );
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			SessionContextImplementor context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		return new SearchQueryResultDefinitionContextImpl<>( searchTargetContext, context,
				documentReferenceTransformer, objectLoader );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new SearchPredicateFactoryContextImpl<>( searchTargetContext.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new SearchTargetSortRootContext<>( searchTargetContext.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext projection() {
		return new SearchProjectionFactoryContextImpl( searchTargetContext.getSearchProjectionFactory() );
	}
}
