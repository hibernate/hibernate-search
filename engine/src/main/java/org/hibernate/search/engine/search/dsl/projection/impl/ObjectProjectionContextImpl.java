/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ObjectProjectionContext;
import org.hibernate.search.engine.search.projection.spi.ObjectProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class ObjectProjectionContextImpl<O> implements ObjectProjectionContext<O> {

	private final ObjectProjectionBuilder<O> objectProjectionBuilder;

	ObjectProjectionContextImpl(SearchProjectionBuilderFactory factory) {
		this.objectProjectionBuilder = factory.object();
	}

	@Override
	/*
	 * The backend has no control over the type of loaded objects.
	 * This cast is only safe because we make sure to only use SearchProjectionFactoryContext
	 * with generic type arguments that are consistent with the type of object loaders.
	 * See comments in MappedIndexSearchTarget.
	 */
	public SearchProjection<O> toProjection() {
		return objectProjectionBuilder.build();
	}

}
