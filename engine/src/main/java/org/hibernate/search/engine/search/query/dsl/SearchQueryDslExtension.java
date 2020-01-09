/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl;


import java.util.Optional;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQueryOptionsStep;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**
 * An extension to the search query DSL, allowing to set non-standard options on a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended steps in the search query definition DSL. Should generally extend
 * {@link SearchQueryHitTypeStep}.
 * @param <R> The reference type.
 * @param <E> The entity type.
 * @param <LOS> The type of the initial step of the loading options definition DSL.
 *
 * @see SearchQueryHitTypeStep#extension(SearchQueryDslExtension)
 * @see AbstractSearchQueryOptionsStep
 */
public interface SearchQueryDslExtension<T, R, E, LOS> {

	/**
	 * Attempt to extend a given DSL step, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchQueryHitTypeStep}.
	 * @param indexScope An {@link IndexScope}.
	 * @param sessionContext A {@link BackendSessionContext}.
	 * @param loadingContextBuilder A {@link LoadingContextBuilder}.
	 * @return An optional containing the extended search query DSL step ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchQueryHitTypeStep<?, R, E, LOS, ?, ?> original,
			IndexScope<?> indexScope,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder);

}
