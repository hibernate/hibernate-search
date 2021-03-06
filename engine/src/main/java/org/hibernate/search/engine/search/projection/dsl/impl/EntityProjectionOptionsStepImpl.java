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
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;


public final class EntityProjectionOptionsStepImpl<E>
		implements EntityProjectionOptionsStep<EntityProjectionOptionsStepImpl<E>, E> {

	private final EntityProjectionBuilder<E> entityProjectionBuilder;

	public EntityProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.entityProjectionBuilder = dslContext.scope().projectionBuilders().entity();
	}

	@Override
	/*
	 * The backend has no control over the type of entities.
	 * This cast is only safe because we make sure to only use SearchProjectionFactory
	 * with generic type arguments that are consistent with the type of entity loaders.
	 * See comments in MappedIndexScope.
	 */
	public SearchProjection<E> toProjection() {
		return entityProjectionBuilder.build();
	}

}
