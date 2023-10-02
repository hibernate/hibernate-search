/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.query.dsl.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedTermMatchingContext implements TermMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;
	private final FieldsContext fieldsContext;

	public ConnectedTermMatchingContext(TermQueryContext termContext,
			String field, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.termContext = termContext;
		this.fieldsContext = new FieldsContext( new String[] { field }, queryContext );
	}

	public ConnectedTermMatchingContext(TermQueryContext termContext,
			String[] fields, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.termContext = termContext;
		this.fieldsContext = new FieldsContext( fields, queryContext );
	}

	@Override
	public TermTermination matching(Object value) {
		switch ( termContext.getApproximation() ) {
			case WILDCARD:
				return new ConnectedMultiFieldsWildcardQueryBuilder( queryContext, queryCustomizer, fieldsContext,
						value );
			case EXACT:
			case FUZZY:
			default:
				return new ConnectedMultiFieldsMatchQueryBuilder( queryContext, queryCustomizer, fieldsContext, value,
						termContext );
		}
	}

	@Override
	public TermMatchingContext andField(String field) {
		fieldsContext.add( field );
		return this;
	}

	@Override
	public TermMatchingContext boostedTo(float boost) {
		fieldsContext.boostedTo( boost );
		return this;
	}

	@Override
	public TermMatchingContext ignoreAnalyzer() {
		fieldsContext.ignoreAnalyzer();
		return this;
	}

	@Override
	public TermMatchingContext ignoreFieldBridge() {
		fieldsContext.ignoreFieldBridge();
		return this;
	}
}
