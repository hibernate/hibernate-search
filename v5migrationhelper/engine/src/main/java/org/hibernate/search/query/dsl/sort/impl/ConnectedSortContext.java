/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortContext;
import org.hibernate.search.query.dsl.sort.SortDistanceNoFieldContext;
import org.hibernate.search.query.dsl.sort.SortFieldContext;
import org.hibernate.search.query.dsl.sort.SortNativeContext;
import org.hibernate.search.query.dsl.sort.SortOrderTermination;
import org.hibernate.search.query.dsl.sort.SortScoreContext;

import org.apache.lucene.search.SortField;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortContext extends AbstractConnectedSortContext implements SortContext {

	public ConnectedSortContext(QueryBuildingContext queryContext) {
		super( queryContext, new SortFieldStates( queryContext ) );
	}

	@Override
	public SortScoreContext byScore() {
		states.setCurrentType( SortField.Type.SCORE );
		return new ConnectedSortScoreContext( queryContext, states );
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
	public SortDistanceNoFieldContext byDistance() {
		return new ConnectedSortDistanceNoFieldContext( queryContext, states );
	}

	@Override
	public SortNativeContext byNative(SortField sortField) {
		states.setCurrentSortFieldNativeSortDescription( sortField );
		return new ConnectedSortNativeContext( queryContext, states );
	}

}
