/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SingleHighlightProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
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

	@SuppressWarnings("deprecation")
	@Override
	public SingleHighlightProjectionFinalStep single() {
		return new SingleHighlightProjectionFinalStepImpl();
	}

	@Override
	public <R> ProjectionFinalStep<R> accumulator(
			org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<String, R> accumulator) {
		return new AccumulatorHighlightProjectionFinalStepImpl<>( accumulator );
	}

	@Override
	public SearchProjection<List<String>> toProjection() {
		return highlight.build( ProjectionAccumulator.list() );
	}

	private class SingleHighlightProjectionFinalStepImpl extends AccumulatorHighlightProjectionFinalStepImpl<String>
			implements SingleHighlightProjectionFinalStep {
		public SingleHighlightProjectionFinalStepImpl() {
			super( ProjectionAccumulator.nullable() );
		}
	}

	private class AccumulatorHighlightProjectionFinalStepImpl<V> implements ProjectionFinalStep<V> {
		private final ProjectionAccumulator.Provider<String, V> accumulatorProvider;

		private AccumulatorHighlightProjectionFinalStepImpl(ProjectionAccumulator.Provider<String, V> accumulatorProvider) {
			this.accumulatorProvider = accumulatorProvider;
		}

		@Override
		public SearchProjection<V> toProjection() {
			return highlight.build( accumulatorProvider );
		}
	}
}
