/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.query.dsl.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedTermMatchingContext implements TermMatchingContext, FieldBridgeCustomization<TermMatchingContext> {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;
	private final FieldsContext fieldsContext;

	public ConnectedTermMatchingContext(TermQueryContext termContext,
			String field, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.termContext = termContext;
		this.fieldsContext = new FieldsContext( new String[] { field } );
	}

	public ConnectedTermMatchingContext(TermQueryContext termContext,
			String[] fields, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.termContext = termContext;
		this.fieldsContext = new FieldsContext( fields );
	}

	@Override
	public TermTermination matching(Object value) {
		return new ConnectedMultiFieldsTermQueryBuilder( termContext, value, fieldsContext, queryCustomizer, queryContext);
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

	@Override
	public TermMatchingContext withFieldBridge(FieldBridge fieldBridge) {
		fieldsContext.withFieldBridge( fieldBridge );
		return this;
	}
}
