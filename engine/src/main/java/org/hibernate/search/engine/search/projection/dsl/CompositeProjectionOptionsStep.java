/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The final step in a composite projection definition
 * where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <T> The type of composed projections.
 */
public interface CompositeProjectionOptionsStep<S extends CompositeProjectionOptionsStep<?, T>, T>
		extends ProjectionFinalStep<T> {

}
