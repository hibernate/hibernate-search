/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.util.common.function.TriFunction;


public class CompositeProjectionOptionsStepImpl<T>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T>, T> {

	private final CompositeProjectionBuilder<T> compositeProjectionBuilder;

	public CompositeProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext,
			Function<List<?>, T> transformer,
			SearchProjection<?>[] projections) {
		this.compositeProjectionBuilder = dslContext.builderFactory().composite( transformer, projections );
	}

	public <P> CompositeProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext,
			Function<P, T> transformer,
			SearchProjection<P> projection) {
		this.compositeProjectionBuilder = dslContext.builderFactory().composite( transformer, projection );
	}

	public <P1, P2> CompositeProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext,
			BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1,
			SearchProjection<P2> projection2) {
		this.compositeProjectionBuilder = dslContext.builderFactory()
				.composite( transformer, projection1, projection2 );
	}

	public <P1, P2, P3> CompositeProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext,
			TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1,
			SearchProjection<P2> projection2,
			SearchProjection<P3> projection3) {
		this.compositeProjectionBuilder = dslContext.builderFactory()
				.composite( transformer, projection1, projection2, projection3 );
	}

	@Override
	public SearchProjection<T> toProjection() {
		return compositeProjectionBuilder.build();
	}

}
