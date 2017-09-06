/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.clause.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class BooleanQueryClauseBuilderImpl extends AbstractQueryClauseBuilder
		implements BooleanQueryClauseBuilder {

	private static final JsonAccessor<JsonObject> MUST = JsonAccessor.root().property( "must" ).asObject();
	private static final JsonAccessor<JsonObject> MUST_NOT = JsonAccessor.root().property( "must_not" ).asObject();
	private static final JsonAccessor<JsonObject> SHOULD = JsonAccessor.root().property( "should" ).asObject();
	private static final JsonAccessor<JsonObject> FILTER = JsonAccessor.root().property( "filter" ).asObject();

	@Override
	public void must(JsonObject query) {
		MUST.add( getInnerObject(), query );
	}

	@Override
	public void mustNot(JsonObject query) {
		MUST_NOT.add( getInnerObject(), query );
	}

	@Override
	public void should(JsonObject query) {
		SHOULD.add( getInnerObject(), query );
	}

	@Override
	public void filter(JsonObject query) {
		FILTER.add( getInnerObject(), query );
	}

	@Override
	public JsonObject build() {
		JsonObject outerObject = getOuterObject();
		outerObject.add( "bool", getInnerObject() );
		return outerObject;
	}

}
