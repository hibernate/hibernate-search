/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.clause.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class RangeQueryClauseBuilderImpl extends AbstractQueryClauseBuilder implements RangeQueryClauseBuilder {

	private static final JsonAccessor<JsonElement> GT = JsonAccessor.root().property( "gt" );
	private static final JsonAccessor<JsonElement> GTE = JsonAccessor.root().property( "gte" );
	private static final JsonAccessor<JsonElement> LT = JsonAccessor.root().property( "lt" );
	private static final JsonAccessor<JsonElement> LTE = JsonAccessor.root().property( "lte" );

	private final String fieldName;

	private final ElasticsearchFieldFormatter formatter;

	private JsonElement lowerLimit;
	private boolean excludeLowerLimit = false;
	private JsonElement upperLimit;
	private boolean excludeUpperLimit = false;

	public RangeQueryClauseBuilderImpl(String fieldName, ElasticsearchFieldFormatter formatter) {
		this.fieldName = fieldName;
		this.formatter = formatter;
	}

	@Override
	public void lowerLimit(Object value) {
		this.lowerLimit = formatter.format( value );
	}

	@Override
	public void excludeLowerLimit() {
		this.excludeLowerLimit = true;
	}

	@Override
	public void upperLimit(Object value) {
		this.upperLimit = formatter.format( value );
	}

	@Override
	public void excludeUpperLimit() {
		this.excludeUpperLimit = true;
	}

	@Override
	public JsonObject build() {
		JsonObject innerObject = getInnerObject();
		JsonAccessor<JsonElement> accessor;
		if ( lowerLimit != null ) {
			accessor = excludeLowerLimit ? GT : GTE;
			accessor.set( innerObject, lowerLimit );
		}
		if ( upperLimit != null ) {
			accessor = excludeUpperLimit ? LT : LTE;
			accessor.set( innerObject, upperLimit );
		}

		JsonObject outerObject = getOuterObject();
		JsonObject middleObject = new JsonObject();
		middleObject.add( fieldName, innerObject );
		outerObject.add( "range", middleObject );
		return outerObject;
	}

}
