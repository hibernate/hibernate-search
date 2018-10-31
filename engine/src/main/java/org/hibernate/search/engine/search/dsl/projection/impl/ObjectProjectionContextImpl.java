/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ObjectProjectionContext;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class ObjectProjectionContextImpl implements ObjectProjectionContext {

	private ObjectSearchProjectionBuilder objectProjectionBuilder;

	ObjectProjectionContextImpl(SearchProjectionBuilderFactory factory) {
		this.objectProjectionBuilder = factory.object();
	}

	@Override
	public SearchProjection<Object> toProjection() {
		return objectProjectionBuilder.build();
	}

}
