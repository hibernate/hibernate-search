/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetContinuationContext;
import org.hibernate.search.query.dsl.FacetFieldContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetFieldContext implements FacetFieldContext {
	private final FacetBuildingContext context;

	public ConnectedFacetFieldContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetContinuationContext onField(String fieldName) {
		context.setFieldName( fieldName );
		return new ConnectedFacetContinuationContext( context );
	}
}

