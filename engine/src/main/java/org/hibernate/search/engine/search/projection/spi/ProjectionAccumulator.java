/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

/**
 * A variation on {@link java.util.stream.Collector} suitable for projections on field values.
 * <p>
 * Compared to {@link java.util.stream.Collector}:
 * <ul>
 *     <li>There is no concept of parallel execution.</li>
 *     <li>All operations are expected to be non-blocking,
 *     except for {@link #transformAll(Object, ProjectionConverter, FromDocumentValueConvertContext)}
 *     and {@link #finish(Object)}.</li>
 *     <li>Values to accumulate are expected to be {@link #transform(Object, int, Object) transformed} exactly once
 *     after accumulation, changing their type from {@link E} to {@link V}.
 *     Clients are responsible for ensuring values to accumulate have been transformed
 *     upon calling {@link #finish(Object)}.
 * </ul>
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 * @param <A> The type of the temporary storage for accumulated values,
 * before and after being transformed.
 * @param <R> The type of the final result containing values of type {@code V}.
 */
public interface ProjectionAccumulator<E, V, A, R> {

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionAccumulator.Provider<V, V> single() {
		return SingleValuedProjectionAccumulator.PROVIDER;
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> Provider<V, List<V>> list() {
		return ListProjectionAccumulator.PROVIDER;
	}

	/**
	 * Creates the initial accumulated container.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @return The initial accumulated container,
	 * to pass to the first call to {@link #accumulate(Object, Object)}.
	 */
	A createInitial();

	/**
	 * Folds a new value in the given accumulated container.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @param accumulated The accumulated value so far.
	 * For the first call, this is a value returned by {@link #createInitial()}.
	 * For the next calls, this is the value returned by the previous call to {@link #accumulate(Object, Object)}.
	 * @param value The value to accumulate.
	 * @return The new accumulated value.
	 */
	A accumulate(A accumulated, E value);

	/**
	 * Folds a collection of new values in the given accumulated container.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @param accumulated The accumulated value so far.
	 * For the first call, this is a value returned by {@link #createInitial()}.
	 * For the next calls, this is the value returned by the previous call to {@link #accumulate(Object, Object)}.
	 * @param values The values to accumulate.
	 * @return The new accumulated value.
	 */
	default A accumulateAll(A accumulated, Collection<E> values) {
		for ( E value : values ) {
			accumulated = accumulate( accumulated, value );
		}
		return accumulated;
	}

	/**
	 * @param accumulated The accumulated value so far,
	 * returned by the last call to {@link #accumulate(Object, Object)}.
	 * @return The number of elements in the accumulated value.
	 */
	int size(A accumulated);

	/**
	 * Retrieves the value at the given index.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @param accumulated The accumulated value so far,
	 * returned by the last call to {@link #accumulate(Object, Object)}.
	 * @param index The index of the value to retrieve.
	 * @return The value at the given index.
	 */
	E get(A accumulated, int index);

	/**
	 * Transforms the value at the given index,
	 * replacing it with the given transformed value.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @param accumulated The accumulated value so far,
	 * returned by the last call to {@link #accumulate(Object, Object)}.
	 * @param index The index of the value being transformed.
	 * @param transformed The transformed value.
	 * @return The new accumulated value.
	 */
	A transform(A accumulated, int index, V transformed);

	/**
	 * Transforms all values with the given converter and the given context.
	 * <p>
	 * This operation may be blocking.
	 *
	 * @param accumulated The accumulated value so far,
	 * returned by the last call to {@link #accumulate(Object, Object)}.
	 * @param converter The projection converter (from {@code F} to {@code V}).
	 * @param context The context to be passed to the projection converter.
	 * @return The new accumulated value.
	 */
	default A transformAll(A accumulated, ProjectionConverter<? super E, ? extends V> converter,
			FromDocumentValueConvertContext context) {
		for ( int i = 0; i < size( accumulated ); i++ ) {
			E initial = get( accumulated, i );
			V transformed = converter.fromDocumentValue( initial, context );
			accumulated = transform( accumulated, i, transformed );
		}
		return accumulated;
	}

	/**
	 * Finishes the accumulation, converting the accumulated container into the final result.
	 * <p>
	 * This operation may be blocking.
	 *
	 * @param accumulated The temporary storage created by {@link #createInitial()},
	 * then populated by successive calls to {@link #accumulate(Object, Object)},
	 * then transformed by a single call to {@link #transformAll(Object, ProjectionConverter, FromDocumentValueConvertContext)}
	 * or by successive calls to {@link #transform(Object, int, Object)}.
	 * @return The final result of the accumulation.
	 */
	R finish(A accumulated);

	/**
	 * Provides an accumulator for a given type of values to accumulate ({@code T}).
	 * <p>
	 * The provider may always return the same accumulator,
	 * if generics are irrelevant and it's safe to do so.
	 *
	 * @param <U> The type of values to accumulate after being transformed.
	 * @param <R> The type of the final result containing values of type {@code V}.
	 */
	interface Provider<U, R> {
		/**
		 * @param <T> The type of values to accumulate before being transformed.
		 * @return An accumulator for the given type.
		 */
		<T> ProjectionAccumulator<T, U, ?, R> get();

		/**
		 * @return {@code true} if accumulators returned by {@link #get()} can only accept a single value,
		 * and will fail beyond that.
		 */
		boolean isSingleValued();
	}

}
