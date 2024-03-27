/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;

/**
 * @author Yoann Rodiere
 */
public class AbstractConnectedSortContext {

	protected final QueryBuildingContext queryContext;
	protected final SortFieldStates states;

	public AbstractConnectedSortContext(QueryBuildingContext queryContext, SortFieldStates states) {
		this.queryContext = queryContext;
		this.states = states;
	}

	protected SortFieldStates getStates() {
		return states;
	}

	protected QueryBuildingContext getQueryContext() {
		return queryContext;
	}

}
