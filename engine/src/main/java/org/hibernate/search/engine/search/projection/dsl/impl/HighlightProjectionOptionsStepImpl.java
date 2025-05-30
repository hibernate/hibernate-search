/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
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

	@Deprecated(since = "8.0")
	@Override
	public SingleHighlightProjectionFinalStep single() {
		return new SingleHighlightProjectionFinalStepImpl();
	}

	@Override
	public <R> ProjectionFinalStep<R> collector(
			ProjectionCollector.Provider<String, R> collector) {
		return new CollectorHighlightProjectionFinalStepImpl<>( collector );
	}

	@Override
	public SearchProjection<List<String>> toProjection() {
		return highlight.build( ProjectionCollector.list() );
	}

	private class SingleHighlightProjectionFinalStepImpl extends CollectorHighlightProjectionFinalStepImpl<String>
			implements SingleHighlightProjectionFinalStep {
		public SingleHighlightProjectionFinalStepImpl() {
			super( ProjectionCollector.nullable() );
		}
	}

	private class CollectorHighlightProjectionFinalStepImpl<V> implements ProjectionFinalStep<V> {
		private final ProjectionCollector.Provider<String, V> collectorProvider;

		private CollectorHighlightProjectionFinalStepImpl(ProjectionCollector.Provider<String, V> collectorProvider) {
			this.collectorProvider = collectorProvider;
		}

		@Override
		public SearchProjection<V> toProjection() {
			return highlight.build( collectorProvider );
		}
	}
}
