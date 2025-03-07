/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.search.projection.spi.BuiltInProjectionCollectors;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A variation on {@link java.util.stream.Collector} suitable for projections on field values.
 * <p>
 * Compared to {@link java.util.stream.Collector}:
 * <ul>
 *     <li>There is no concept of parallel execution.</li>
 *     <li>All operations are expected to be non-blocking,
 *     except for {@link #transformAll(Object, FromDocumentValueConverter, FromDocumentValueConvertContext)}
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
@Incubating
public interface ProjectionCollector<E, V, A, R> {

	/**
	 * @return The projection collector capable of accumulating single-valued projections in an as-is form,
	 * i.e. the value is returned without any extra transformations.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> ProjectionCollector.Provider<V, V> nullable() {
		return BuiltInProjectionCollectors.nullable();
	}

	/**
	 * @return The projection collector capable of accumulating single-valued projections and wrapping the values in an {@link Optional}.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, Optional<V>> optional() {
		return BuiltInProjectionCollectors.optional();
	}

	/**
	 * @param converter The function that defines how to convert a list of collected values to the final collection.
	 * @return An collector based on a list as a temporary storage.
	 * @param <V> The type of values to accumulate.
	 * @param <C> The type of the resulting collection.
	 */
	static <V, C> Provider<V, C> simple(Function<List<V>, C> converter) {
		return BuiltInProjectionCollectors.simple( converter );
	}

	/**
	 * @return The projection collector capable of accumulating multivalued projections as a {@link List}.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, List<V>> list() {
		return BuiltInProjectionCollectors.list();
	}

	/**
	 * @return The projection collector capable of accumulating multivalued projections as a {@link Set}.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, Set<V>> set() {
		return BuiltInProjectionCollectors.set();
	}

	/**
	 * @return The projection collector capable of accumulating multivalued projections as a {@link SortedSet}.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, SortedSet<V>> sortedSet() {
		return BuiltInProjectionCollectors.sortedSet();
	}

	/**
	 * @return The projection collector capable of accumulating multivalued projections as a {@link SortedSet}
	 * using a custom comparator.
	 * @param comparator The comparator which should be used by the sorted set.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, SortedSet<V>> sortedSet(Comparator<? super V> comparator) {
		return BuiltInProjectionCollectors.sortedSet( comparator );
	}

	/**
	 * @return The projection collector capable of accumulating multivalued projections as an array.
	 * @param componentType The type of the array elements.
	 * @param <V> The type of values to accumulate.
	 */
	static <V> Provider<V, V[]> array(Class<? super V> componentType) {
		return BuiltInProjectionCollectors.array( componentType );
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
	default A transformAll(A accumulated, FromDocumentValueConverter<? super E, ? extends V> converter,
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
	 * then transformed by a single call to {@link #transformAll(Object, FromDocumentValueConverter, FromDocumentValueConvertContext)}
	 * or by successive calls to {@link #transform(Object, int, Object)}.
	 * @return The final result of the accumulation.
	 */
	R finish(A accumulated);

	/**
	 * @return An "empty" final value, i.e. when a {@link #finish(Object) final transformation}
	 * is applied to {@link #createInitial() the initial value}.
	 */
	default R empty() {
		return finish( createInitial() );
	}

	/**
	 * Provides a collector for a given type of values to accumulate ({@code T}).
	 * <p>
	 * The provider may always return the same collector,
	 * if generics are irrelevant and it's safe to do so.
	 *
	 * @param <U> The type of values to accumulate after being transformed.
	 * @param <R> The type of the final result containing values of type {@code V}.
	 */
	interface Provider<U, R> {
		/**
		 * @param <T> The type of values to accumulate before being transformed.
		 * @return A collector for the given type.
		 */
		<T> ProjectionCollector<T, U, ?, R> get();

		/**
		 * @return {@code true} if collectors returned by {@link #get()} can only accept a single value,
		 * and will fail beyond that.
		 */
		boolean isSingleValued();
	}

}
