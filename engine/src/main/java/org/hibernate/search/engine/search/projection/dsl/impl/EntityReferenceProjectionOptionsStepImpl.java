/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public final class EntityReferenceProjectionOptionsStepImpl<R>
		implements EntityReferenceProjectionOptionsStep<EntityReferenceProjectionOptionsStepImpl<R>, R> {

	private final SearchProjection<R> entityReferenceProjection;

	public EntityReferenceProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.entityReferenceProjection = dslContext.scope().projectionBuilders().entityReference();
	}

	@Override
	public SearchProjection<R> toProjection() {
		return entityReferenceProjection;
	}

}
