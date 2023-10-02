/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;

public interface CompositeProjectionBuilder {

	<E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
			ProjectionAccumulator.Provider<V, P> accumulatorProvider);

}
