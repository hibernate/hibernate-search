/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;


public final class EntityProjectionOptionsStepImpl<E>
		implements EntityProjectionOptionsStep<EntityProjectionOptionsStepImpl<E>, E> {

	private final SearchProjection<E> entityProjection;

	public EntityProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.entityProjection = dslContext.scope().projectionBuilders().entity();
	}

	@Override
	public SearchProjection<E> toProjection() {
		return entityProjection;
	}

}
