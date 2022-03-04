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
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.util.common.function.TriFunction;


public class CompositeProjectionValueStepImpl<T>
		extends CompositeProjectionOptionsStepImpl<T>
		implements CompositeProjectionValueStep<CompositeProjectionOptionsStepImpl<T>, T> {

	public CompositeProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext,
			Function<List<?>, T> transformer,
			SearchProjection<?>[] projections) {
		super( dslContext.scope().projectionBuilders().composite( transformer, projections ) );
	}

	public <P> CompositeProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext,
			Function<P, T> transformer,
			SearchProjection<P> projection) {
		super( dslContext.scope().projectionBuilders().composite( transformer, projection ) );
	}

	public <P1, P2> CompositeProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext,
			BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1,
			SearchProjection<P2> projection2) {
		super( dslContext.scope().projectionBuilders()
				.composite( transformer, projection1, projection2 ) );
	}

	public <P1, P2, P3> CompositeProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext,
			TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1,
			SearchProjection<P2> projection2,
			SearchProjection<P3> projection3) {
		super( dslContext.scope().projectionBuilders()
				.composite( transformer, projection1, projection2, projection3 ) );
	}

	@Override
	public CompositeProjectionOptionsStep<?, List<T>> multi() {
		// TODO HSEARCH-3943 implement multi()
		throw new IllegalStateException( "Not implemented yet" );
	}
}
