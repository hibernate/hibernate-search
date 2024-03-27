/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The initial and final step in an "id" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <I> The type of the identifier.
 */
public interface IdProjectionOptionsStep<S extends IdProjectionOptionsStep<?, I>, I>
		extends ProjectionFinalStep<I> {

}
