/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;
import org.hibernate.search.query.dsl.SimpleQueryStringDefinitionTermination;
import org.hibernate.search.query.dsl.SimpleQueryStringTermination;

/**
 * @author Guillaume Smet
 */
public class ConnectedSimpleQueryStringMatchingContext implements SimpleQueryStringMatchingContext {

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;

	private final List<FieldsContext> fieldsContexts = new ArrayList<FieldsContext>();
	private FieldsContext currentFieldsContext;

	private boolean withAndAsDefaultOperator = false;

	public ConnectedSimpleQueryStringMatchingContext(String field, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;

		addFields( field );
	}

	public ConnectedSimpleQueryStringMatchingContext(String[] fields, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;

		addFields( fields );
	}

	@Override
	public SimpleQueryStringMatchingContext andField(String field) {
		addFields( field );
		return this;
	}

	@Override
	public SimpleQueryStringMatchingContext andFields(String... fields) {
		addFields( fields );
		return this;
	}

	@Override
	public SimpleQueryStringMatchingContext boostedTo(float boost) {
		currentFieldsContext.boostedTo( boost );
		return this;
	}

	@Override
	public SimpleQueryStringTermination matching(String simpleQueryString) {
		return new ConnectedMultiFieldsSimpleQueryStringQueryBuilder( simpleQueryString, fieldsContexts, withAndAsDefaultOperator, queryCustomizer, queryContext );
	}

	@Override
	public SimpleQueryStringDefinitionTermination withAndAsDefaultOperator() {
		withAndAsDefaultOperator = true;
		return this;
	}

	private void addFields(String... fields) {
		FieldsContext fieldsContext = new FieldsContext( fields, queryContext );
		this.fieldsContexts.add( fieldsContext );
		this.currentFieldsContext = fieldsContext;
	}

}
