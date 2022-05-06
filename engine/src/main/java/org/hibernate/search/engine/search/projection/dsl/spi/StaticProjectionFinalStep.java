/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;

public final class StaticProjectionFinalStep<T> implements ProjectionFinalStep<T> {
	private final SearchProjection<T> projection;

	public StaticProjectionFinalStep(SearchProjection<T> projection) {
		this.projection = projection;
	}

	@Override
	public SearchProjection<T> toProjection() {
		return projection;
	}
}
