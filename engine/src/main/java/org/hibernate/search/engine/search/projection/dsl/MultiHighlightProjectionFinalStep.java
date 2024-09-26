/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The final step in a multi-valued highlight definition, where the collection {@code C} can be different from the default {@link java.util.List}.
 */
public interface MultiHighlightProjectionFinalStep<C> extends ProjectionFinalStep<C> {

}
