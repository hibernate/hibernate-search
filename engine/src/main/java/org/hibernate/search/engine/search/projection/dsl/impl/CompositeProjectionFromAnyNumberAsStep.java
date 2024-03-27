/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromAsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;

class CompositeProjectionFromAnyNumberAsStep extends AbstractCompositeProjectionFromAsStep
		implements CompositeProjectionFromAsStep {

	final SearchProjection<?>[] inner;

	public CompositeProjectionFromAnyNumberAsStep(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inner) {
		super( builder );
		this.inner = inner;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return inner;
	}

}
