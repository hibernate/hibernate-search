/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.IdProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public final class IdProjectionOptionsStepImpl<I> implements IdProjectionOptionsStep<IdProjectionOptionsStepImpl<I>, I> {

	private final SearchProjection<I> idProjection;

	public IdProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext, Class<I> identifierType) {
		idProjection = dslContext.scope().projectionBuilders().id( identifierType );
	}

	@Override
	public SearchProjection<I> toProjection() {
		return idProjection;
	}
}
