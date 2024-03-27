/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The initial and final step in a "field" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <T> The type of projected field values.
 */
public interface FieldProjectionOptionsStep<S extends FieldProjectionOptionsStep<?, T>, T>
		extends ProjectionFinalStep<T> {

}
