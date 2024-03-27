/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
