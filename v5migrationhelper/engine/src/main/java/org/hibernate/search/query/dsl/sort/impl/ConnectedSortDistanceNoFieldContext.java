/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortDistanceFieldContext;
import org.hibernate.search.query.dsl.sort.SortDistanceNoFieldContext;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public class ConnectedSortDistanceNoFieldContext extends AbstractConnectedSortContext
		implements SortDistanceNoFieldContext {

	public ConnectedSortDistanceNoFieldContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortDistanceFieldContext onField(String fieldName) {
		getStates().setCurrentName( fieldName );
		return new ConnectedSortDistanceFieldContext( queryContext, states );
	}

}
