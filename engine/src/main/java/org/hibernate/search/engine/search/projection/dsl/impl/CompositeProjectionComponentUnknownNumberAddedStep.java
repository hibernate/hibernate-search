/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

class CompositeProjectionComponentUnknownNumberAddedStep
		extends AbstractCompositeProjectionComponentsAtLeastOneAddedStep {

	final SearchProjection<?>[] components;

	public CompositeProjectionComponentUnknownNumberAddedStep(SearchProjectionDslContext<?> dslContext,
			SearchProjection<?>[] components) {
		super( dslContext );
		this.components = components;
	}

	@Override
	SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections) {
		SearchProjection<?>[] array = new SearchProjection[1 + otherProjections.length];
		System.arraycopy( components, 0, array, 0, components.length );
		System.arraycopy( otherProjections, 0, array, components.length, otherProjections.length );
		return array;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return components;
	}

}
