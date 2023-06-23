/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

public class CompositeProjectionOptionsStepImpl<T, P>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T, P>, P> {

	final CompositeProjectionBuilder builder;
	final SearchProjection<?>[] inners;
	final ProjectionCompositor<?, T> compositor;
	private final ProjectionAccumulator.Provider<T, P> accumulatorProvider;

	public CompositeProjectionOptionsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ProjectionCompositor<?, T> compositor,
			ProjectionAccumulator.Provider<T, P> accumulatorProvider) {
		this.builder = builder;
		this.inners = inners;
		this.compositor = compositor;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return builder.build( inners, compositor, accumulatorProvider );
	}
}
