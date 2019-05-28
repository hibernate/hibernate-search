/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ReferenceProjectionContext;
import org.hibernate.search.engine.search.projection.spi.ReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class ReferenceProjectionContextImpl<R> implements ReferenceProjectionContext<R> {

	private final ReferenceProjectionBuilder<R> referenceProjectionBuilder;

	ReferenceProjectionContextImpl(SearchProjectionBuilderFactory factory) {
		this.referenceProjectionBuilder = factory.reference();
	}

	@Override
	/*
	 * The backend has no control over the type of entities.
	 * This cast is only safe because we make sure to only use SearchProjectionFactoryContext
	 * with generic type arguments that are consistent with the type of entity loaders.
	 * See comments in MappedIndexScope.
	 */
	public SearchProjection<R> toProjection() {
		return referenceProjectionBuilder.build();
	}

}
