/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.SearchException;

/**
 * A factory for search projection builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search projections.
 */
public interface SearchProjectionBuilderFactory {

	SearchProjection<DocumentReference> documentReference();

	<E> SearchProjection<E> entityLoading();

	<R> SearchProjection<R> entityReference();

	<I> SearchProjection<I> id(Class<I> identifierType);

	SearchProjection<Float> score();

	CompositeProjectionBuilder composite();

	<T> SearchProjection<T> constant(T value);

	<T> SearchProjection<T> entityComposite(SearchProjection<T> delegate);

	/**
	 * @param exceptionSupplier A supplier of the exception to throw.
	 * @return A projection that throws an exception as soon as it's applied to at least one document.
	 * @param <T> The type of projected values.
	 */
	<T> SearchProjection<T> throwing(Supplier<SearchException> exceptionSupplier);

	/**
	 * @param inners A map from type name to projection.
	 * @return A projection that delegates to the given projections,
	 * picking the delegate based on the document's type name.
	 * @param <T> The type of projected values.
	 */
	<T> SearchProjection<T> byTypeName(Map<String, ? extends SearchProjection<? extends T>> inners);

}
