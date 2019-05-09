/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**
 * @param <R> The type of references, i.e. the type of hits returned by
 * {@link SearchQueryResultDefinitionContext#asReference() reference queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#reference() reference projections}.
 * @param <O> The type of entities, i.e. the type of hits returned by
 * {@link SearchQueryResultDefinitionContext#asEntity() entity queries}
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 */
public interface MappedIndexSearchScope<R, O> {

	/*
	 * IMPLEMENTATION NOTE: we *must* only accept a loading context with the same R/O type parameters as this class,
	 * otherwise some casts in EntityProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 * In particular, we cannot accept a LoadingContextBuilder<R, T> with any T.
	 */
	SearchQueryResultDefinitionContext<R, O, SearchProjectionFactoryContext<R, O>> search(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	/*
	 * IMPLEMENTATION NOTE: we *must* return a factory with the same R/O type arguments as this class,
	 * otherwise some casts in EntityProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 */
	SearchProjectionFactoryContext<R, O> projection();

}
