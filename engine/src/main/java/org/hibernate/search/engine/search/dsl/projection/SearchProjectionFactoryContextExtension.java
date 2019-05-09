/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection;


import java.util.Optional;

import org.hibernate.search.engine.search.dsl.projection.spi.DelegatingSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

/**
 * An extension to the search query DSL, allowing to add non-standard projections to a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended search factory contexts. Should generally extend
 * {@link SearchProjectionFactoryContext}.
 * @param <R> The type of references in the original {@link SearchProjectionFactoryContext}.
 * @param <O> The type of entities in the original {@link SearchProjectionFactoryContext}.
 *
 * @see SearchProjectionFactoryContext#extension(SearchProjectionFactoryContextExtension)
 * @see DelegatingSearchProjectionFactoryContext
 */
public interface SearchProjectionFactoryContextExtension<T, R, O> {

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchProjectionFactoryContext}.
	 * @param factory A {@link SearchProjectionBuilderFactory}.
	 * @return An optional containing the extended search projection factory context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchProjectionFactoryContext<R, O> original,
			SearchProjectionBuilderFactory factory);

}
