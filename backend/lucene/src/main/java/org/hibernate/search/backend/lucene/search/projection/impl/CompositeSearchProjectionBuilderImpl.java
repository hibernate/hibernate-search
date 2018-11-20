/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeSearchProjectionBuilder;


class CompositeSearchProjectionBuilderImpl<T> implements CompositeSearchProjectionBuilder<T> {

	private final CompositeSearchProjection<?, T> projection;

	CompositeSearchProjectionBuilderImpl(CompositeSearchProjection<?, T> projection) {
		this.projection = projection;
	}

	@Override
	public SearchProjection<T> build() {
		return projection;
	}
}
