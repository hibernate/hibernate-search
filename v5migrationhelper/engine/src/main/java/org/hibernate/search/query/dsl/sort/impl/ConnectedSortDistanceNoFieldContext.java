/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
