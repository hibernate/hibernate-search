/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.spi.SearchIntegrator;

/**
 * Keep the query builder contextual information
 *
 * @author Emmanuel Bernard
 */
public class QueryBuildingContext {

	private final SearchIntegrator integrator;

	public QueryBuildingContext(SearchIntegrator integrator) {
		this.integrator = integrator;
	}

	public SearchIntegrator getIntegrator() {
		return integrator;
	}

}
