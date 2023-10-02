/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

/**
 * Binds a constructor parameter to a projection to the score of a hit.
 *
 * @see SearchProjectionFactory#score()
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScoreProjection
 */
public final class ScoreProjectionBinder implements ProjectionBinder {
	private static final ScoreProjectionBinder INSTANCE = new ScoreProjectionBinder();

	/**
	 * Creates a {@link ScoreProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static ScoreProjectionBinder create() {
		return INSTANCE;
	}

	private ScoreProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		context.definition( Float.class, Definition.INSTANCE );
	}

	private static class Definition extends AbstractProjectionDefinition<Float> {
		public static final Definition INSTANCE = new Definition();

		@Override
		protected String type() {
			return "score";
		}

		@Override
		public SearchProjection<Float> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.score().toProjection();
		}
	}

}
