/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;

public class FieldProjectionOptionsStepImpl<T, P>
		implements FieldProjectionOptionsStep<FieldProjectionOptionsStepImpl<T, P>, P> {

	protected final FieldProjectionBuilder<T> fieldProjectionBuilder;
	private final ProjectionCollector.Provider<T, P> collectorProvider;

	FieldProjectionOptionsStepImpl(FieldProjectionBuilder<T> fieldProjectionBuilder,
			ProjectionCollector.Provider<T, P> collectorProvider) {
		this.fieldProjectionBuilder = fieldProjectionBuilder;
		this.collectorProvider = collectorProvider;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return fieldProjectionBuilder.build( collectorProvider );
	}

}
