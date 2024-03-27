/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public final class ScoreProjectionOptionsStepImpl
		implements ScoreProjectionOptionsStep<ScoreProjectionOptionsStepImpl> {

	private final SearchProjection<Float> scoreProjection;

	public ScoreProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.scoreProjection = dslContext.scope().projectionBuilders().score();
	}

	@Override
	public SearchProjection<Float> toProjection() {
		return scoreProjection;
	}

}
