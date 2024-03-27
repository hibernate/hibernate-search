/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The initial and final step in an "entity" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <E> The type of projected entities.
 */
public interface EntityProjectionOptionsStep<S extends EntityProjectionOptionsStep<?, E>, E>
		extends ProjectionFinalStep<E> {

}
