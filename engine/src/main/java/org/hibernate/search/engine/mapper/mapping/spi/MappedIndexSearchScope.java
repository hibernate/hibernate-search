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
 * @param <E> The type of entities, i.e. the type of hits returned by
 * {@link SearchQueryResultDefinitionContext#asEntity() entity queries}
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 */
public interface MappedIndexSearchScope<R, E> {

	/*
	 * IMPLEMENTATION NOTE: we *must* only accept a loading context with the same R/E type parameters as this class,
	 * otherwise some casts in EntityProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 * In particular, we cannot accept a LoadingContextBuilder<R, T> with any T.
	 */
	SearchQueryResultDefinitionContext<?, R, E, SearchProjectionFactoryContext<R, E>, ?> search(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	/*
	 * IMPLEMENTATION NOTE: we *must* return a factory with the same R/E type arguments as this class,
	 * otherwise some casts in EntityProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 */
	SearchProjectionFactoryContext<R, E> projection();

}
