/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
