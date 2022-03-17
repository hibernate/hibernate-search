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
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;


public class CompositeProjectionOptionsStepImpl<T>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T>, T> {

	private final CompositeProjectionBuilder builder;
	private final SearchProjection<?>[] inners;
	private final ProjectionCompositor<?, T> compositor;

	public CompositeProjectionOptionsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ProjectionCompositor<?, T> compositor) {
		this.builder = builder;
		this.inners = inners;
		this.compositor = compositor;
	}

	@Override
	public SearchProjection<T> toProjection() {
		return builder.build( inners, compositor );
	}
}
