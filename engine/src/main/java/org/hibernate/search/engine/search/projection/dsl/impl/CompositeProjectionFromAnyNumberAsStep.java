/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionAsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;

class CompositeProjectionFromAnyNumberAsStep extends AbstractCompositeProjectionAsStep
		implements CompositeProjectionAsStep {

	final SearchProjection<?>[] inner;

	public CompositeProjectionFromAnyNumberAsStep(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inner) {
		super( builder );
		this.inner = inner;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return inner;
	}

}
