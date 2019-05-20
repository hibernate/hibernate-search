/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;


import java.util.Optional;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**

 * An extension to the search query DSL, allowing to add non-standard predicates to a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended search query contexts. Should generally extend
 * {@link SearchQueryResultDefinitionContext}.
 * @param <R> The reference type.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 * {@code .extension( ElasticsearchExtension.get() }.
 * @param <E> The entity type.
 * Users should not have to care about this, as the parameter will automatically take the appropriate value when calling
 *
 * @see SearchQueryResultDefinitionContext#extension(SearchQueryContextExtension)
 * @see AbstractSearchQueryContext
 */
public interface SearchQueryContextExtension<T, R, E> {

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchQueryResultContext}.
	 * @param indexSearchScope An {@link IndexSearchScope}.
	 * @param sessionContext A {@link SessionContextImplementor}.
	 * @param loadingContextBuilder A {@link LoadingContextBuilder}.
	 * @return An optional containing the extended search query context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchQueryResultDefinitionContext<?, R, E, ?, ?> original,
			IndexSearchScope<?> indexSearchScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder);

}
