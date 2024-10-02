/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionAccumulator;

/**
 * The initial step in a "field" projection definition,
 * where (optionally) the projection accumulator can be provided, e.g. to mark a projection as multi-valued (returning {@code List}/{@code Set} etc.)
 * or wrapped in some other container (e.g. {@code Optional<..>}),
 * and where optional parameters can be set.
 * <p>
 * By default (if {@link #accumulator(ProjectionAccumulator.Provider)} is not called), the projection is considered single-valued,
 * and its creation will fail if the field is multi-valued.
 *
 * @param <N> The next step if a method other than {@link #accumulator(ProjectionAccumulator.Provider)} is called,
 * i.e. the return type of methods defined in {@link FieldProjectionOptionsStep}
 * when called directly on this object.
 * @param <T> The type of projected field values.
 */
public interface FieldProjectionValueStep<N extends FieldProjectionOptionsStep<?, T>, T>
		extends FieldProjectionOptionsStep<N, T> {

	/**
	 * Defines the projection as multi-valued, i.e. returning {@code List<T>} instead of {@code T}.
	 * <p>
	 * Calling {@link #multi()} is mandatory for multi-valued fields,
	 * otherwise the projection will throw an exception upon creating the query.
	 *
	 * @return A new step to define optional parameters for the multi-valued projections.
	 * @deprecated Use {@link #accumulator(ProjectionAccumulator.Provider)} instead.
	 */
	@Deprecated(since = "8.0")
	default FieldProjectionOptionsStep<?, List<T>> multi() {
		return accumulator( ProjectionAccumulator.list() );
	}

	/**
	 * Defines how to accumulate field projection values.
	 * <p>
	 * Calling {@code .accumulator(someMultiValuedAccumulatorProvider) } is mandatory for multi-valued fields,
	 * e.g. {@code .accumulator(ProjectionAccumulator.list())},
	 * otherwise the projection will throw an exception upon creating the query.
	 *
	 * @param accumulator The accumulator provider to apply to this projection.
	 * @return A new step to define optional parameters for the accumulated projections.
	 * @param <R> The type of the final result.
	 */
	<R> FieldProjectionOptionsStep<?, R> accumulator(ProjectionAccumulator.Provider<T, R> accumulator);
}
