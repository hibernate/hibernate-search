/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.util.common.function.TriFunction;

class CompositeProjectionFrom3AsStepImpl<V1, V2, V3>
		extends AbstractCompositeProjectionAsStep
		implements CompositeProjectionFrom3AsStep<V1, V2, V3> {

	final SearchProjection<V1> inner1;
	final SearchProjection<V2> inner2;
	final SearchProjection<V3> inner3;

	public CompositeProjectionFrom3AsStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> inner1, SearchProjection<V2> inner2, SearchProjection<V3> inner3) {
		super( dslContext );
		this.inner1 = inner1;
		this.inner2 = inner2;
		this.inner3 = inner3;
	}

	@Override
	public <V> CompositeProjectionValueStep<?, V> as(TriFunction<V1, V2, V3, V> transformer) {
		return new CompositeProjectionValueStepImpl<>( dslContext, transformer, inner1, inner2, inner3 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { inner1, inner2, inner3 };
	}

}
