/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetContext;
import org.hibernate.search.query.dsl.FacetFieldContext;

/**
 * @author Hardy Ferentschik
 */
class ConnectedFacetContext implements FacetContext {
	private final FacetBuildingContext context;

	ConnectedFacetContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetFieldContext name(String name) {
		context.setName( name );
		return new ConnectedFacetFieldContext( context );
	}
}

