/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

class CompositeProjectionFrom1AsStepImpl<V1> extends AbstractCompositeProjectionAsStep
		implements CompositeProjectionFrom1AsStep<V1> {

	final SearchProjection<V1> inner1;

	public CompositeProjectionFrom1AsStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> inner1) {
		super( dslContext );
		this.inner1 = inner1;
	}

	@Override
	public <V> CompositeProjectionOptionsStep<?, V> as(Function<V1, V> transformer) {
		return new CompositeProjectionOptionsStepImpl<>( dslContext, transformer, inner1 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { inner1 };
	}
}
