/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

