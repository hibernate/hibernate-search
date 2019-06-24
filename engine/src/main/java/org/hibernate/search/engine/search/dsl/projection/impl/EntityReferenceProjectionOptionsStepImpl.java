/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class EntityReferenceProjectionOptionsStepImpl<R> implements EntityReferenceProjectionOptionsStep<R> {

	private final EntityReferenceProjectionBuilder<R> entityReferenceProjectionBuilder;

	EntityReferenceProjectionOptionsStepImpl(SearchProjectionBuilderFactory factory) {
		this.entityReferenceProjectionBuilder = factory.entityReference();
	}

	@Override
	/*
	 * The backend has no control over the type of entities.
	 * This cast is only safe because we make sure to only use SearchProjectionFactoryContext
	 * with generic type arguments that are consistent with the type of entity loaders.
	 * See comments in MappedIndexScope.
	 */
	public SearchProjection<R> toProjection() {
		return entityReferenceProjectionBuilder.build();
	}

}
