/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

/**
 * A variation on {@link java.util.stream.Collector} suitable for projections on field values.
 * <p>
 * Compared to {@link java.util.stream.Collector}:
 * <ul>
 *     <li>There is no concept of parallel execution.</li>
 *     <li>All operations are expected to be non-blocking,
 *     except for {@link #finish(Object, ProjectionConverter, FromDocumentFieldValueConvertContext)}</li>
 * </ul>
 *
 * @param <F> The type of (unconverted) field values.
 * @param <V> The type of field values after the projection converter was applied.
 * @param <U> The type of the temporary storage for collected field values of type {@code F}.
 * @param <R> The type of the final result containing values of type {@code V}.
 */
public interface ProjectionAccumulator<F, V, U, R> {

	/**
	 * Creates the initial accumulated container.
	 * <p>
	 * This operation should be non-blocking.
	 *
	 * @return The initial accumulated container,
	 * to pass to the first call to {@link #accumulate(Object, Object)}.
	 */
	U createInitial();

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
	U accumulate(U accumulated, F value);

	/**
	 * Finishes the collecting, converting the accumulated container into the final result.
	 * <p>
	 * This operation may be blocking.
	 *
	 * @param accumulated The temporary storage created by {@link #createInitial()} and populated
	 * by successive calls to {@link #accumulate(Object, Object)}.
	 * @param converter The projection converter (from {@code F} to {@code V}).
	 * @param context The context to be passed to the projection converter.
	 * @return The final result of the collecting.
	 */
	R finish(U accumulated, ProjectionConverter<? super F, ? extends V> converter,
			FromDocumentFieldValueConvertContext context);

	/**
	 * Provides an accumulator for a given underlying field type ({@code F}).
	 * <p>
	 * The provider may always return the same accumulator,
	 * if generics are irrelevant and it's safe to do so.
	 *
	 * @param <V> The type of field values after the projection converter was applied.
	 * @param <R> The type of the final result containing values of type {@code V}.
	 */
	interface Provider<V, R> {
		/**
		 * @param <F> The type of field values.
		 * @return An accumulator for the given type.
		 */
		<F> ProjectionAccumulator<F, V, ?, R> get();

		/**
		 * @return {@code true} if accumulators returned by {@link #get()} can only accept a single value,
		 * and will fail beyond that.
		 */
		boolean isSingleValued();
	}

}
