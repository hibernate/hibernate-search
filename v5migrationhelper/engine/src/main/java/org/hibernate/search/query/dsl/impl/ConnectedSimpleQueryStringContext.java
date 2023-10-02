/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.SimpleQueryStringContext;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;

import org.apache.lucene.search.Query;

/**
 * @author Guillaume Smet
 */
class ConnectedSimpleQueryStringContext implements SimpleQueryStringContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;

	public ConnectedSimpleQueryStringContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
	}

	@Override
	public SimpleQueryStringMatchingContext onField(String field) {
		return new ConnectedSimpleQueryStringMatchingContext( field, queryCustomizer, queryContext );
	}

	@Override
	public SimpleQueryStringMatchingContext onFields(String field, String... fields) {
		String[] allFields = new String[fields.length + 1];
		allFields[0] = field;
		System.arraycopy( fields, 0, allFields, 1, fields.length );
		return new ConnectedSimpleQueryStringMatchingContext( allFields, queryCustomizer, queryContext );
	}

	@Override
	public ConnectedSimpleQueryStringContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public ConnectedSimpleQueryStringContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public ConnectedSimpleQueryStringContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
