/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;

public final class StaticProjectionFinalStep<T> implements ProjectionFinalStep<T> {
	private final SearchProjection<T> projection;

	public StaticProjectionFinalStep(SearchProjection<T> projection) {
		this.projection = projection;
	}

	@Override
	public SearchProjection<T> toProjection() {
		return projection;
	}
}
