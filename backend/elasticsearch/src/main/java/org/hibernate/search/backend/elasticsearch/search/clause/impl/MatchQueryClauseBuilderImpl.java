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
class MatchQueryClauseBuilderImpl extends AbstractQueryClauseBuilder implements MatchQueryClauseBuilder {

	private static final JsonAccessor<JsonElement> VALUE = JsonAccessor.root().property( "value" );

	private final String fieldName;

	private final ElasticsearchFieldFormatter formatter;

	public MatchQueryClauseBuilderImpl(String fieldName, ElasticsearchFieldFormatter formatter) {
		this.fieldName = fieldName;
		this.formatter = formatter;
	}

	@Override
	public void value(Object value) {
		VALUE.set( getInnerObject(), formatter.format( value ) );
	}

	@Override
	public JsonObject build() {
		JsonObject outerObject = getOuterObject();
		JsonObject middleObject = new JsonObject();
		middleObject.add( fieldName, getInnerObject() );
		outerObject.add( "match", middleObject );
		return outerObject;
	}

}
