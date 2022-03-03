/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

class CompositeProjectionFrom2AsStepImpl<V1, V2>
		extends AbstractCompositeProjectionAsStep
		implements CompositeProjectionFrom2AsStep<V1, V2> {

	final SearchProjection<V1> inner1;
	final SearchProjection<V2> inner2;

	public CompositeProjectionFrom2AsStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> inner1, SearchProjection<V2> inner2) {
		super( dslContext );
		this.inner1 = inner1;
		this.inner2 = inner2;
	}

	@Override
	public <V> CompositeProjectionOptionsStep<?, V> as(BiFunction<V1, V2, V> transformer) {
		return new CompositeProjectionOptionsStepImpl<>( dslContext, transformer, inner1, inner2 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { inner1, inner2 };
	}

}
