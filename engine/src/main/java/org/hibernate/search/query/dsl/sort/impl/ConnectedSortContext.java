/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortContext;
import org.hibernate.search.query.dsl.sort.SortDistanceContext;
import org.hibernate.search.query.dsl.sort.SortFieldContext;
import org.hibernate.search.query.dsl.sort.SortNativeContext;
import org.hibernate.search.query.dsl.sort.SortOrderTermination;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortContext implements SortContext {
	private final QueryBuildingContext queryContext;
	private final SortFieldStates states;

	public ConnectedSortContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.states = new SortFieldStates(queryContext);
	}

	@Override
	public SortOrderTermination byScore() {
		states.setCurrentType( SortField.Type.SCORE );
		return new ConnectedSortOrderTermination( queryContext, states );
	}

	@Override
	public SortOrderTermination byIndexOrder() {
		states.setCurrentType( SortField.Type.DOC );
		return new ConnectedSortOrderTermination( queryContext, states );
	}

	@Override
	public SortFieldContext byField(String field) {
		states.setCurrentName( field );
		return new ConnectedSortFieldContext( queryContext, states );
	}

	@Override
	public SortDistanceContext byDistanceFromSpatialQuery(Query query) {
		states.setCurrentSpatialQuery(query);
		return new ConnectedSortDistanceContext( queryContext, states );
	}

	@Override
	public SortNativeContext byNative(SortField sortField) {
		states.setCurrentSortField(sortField);
		return new ConnectedSortNativeContext( queryContext, states );
	}

	@Override
	public SortNativeContext byNative(String sortField) {
		states.setCurrentSortField(sortField);
		return new ConnectedSortNativeContext( queryContext, states );
	}

}
