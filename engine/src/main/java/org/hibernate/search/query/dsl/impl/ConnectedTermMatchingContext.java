/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
