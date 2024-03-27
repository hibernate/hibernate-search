/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SingleHighlightProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class HighlightProjectionOptionsStepImpl
		implements HighlightProjectionOptionsStep,
		HighlightProjectionFinalStep {

	private final HighlightProjectionBuilder highlight;

	public HighlightProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath) {
		this.highlight = dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.HIGHLIGHT );
	}

	@Override
	public HighlightProjectionOptionsStep highlighter(String highlighterName) {
		highlight.highlighter( highlighterName );
		return this;
	}

	@Override
	public SingleHighlightProjectionFinalStep single() {
		return new SingleHighlightProjectionFinalStepImpl();
	}

	@Override
	public SearchProjection<List<String>> toProjection() {
		return highlight.build( ProjectionAccumulator.list() );
	}

	private class SingleHighlightProjectionFinalStepImpl implements SingleHighlightProjectionFinalStep {
		@Override
		public SearchProjection<String> toProjection() {
			return highlight.build( ProjectionAccumulator.single() );
		}

	}
}
