/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeMappingStep;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

class ElasticsearchNativeIndexFieldTypeMappingStepImpl
		implements ElasticsearchNativeIndexFieldTypeMappingStep {

	private final ElasticsearchIndexFieldTypeBuildContext buildContext;

	ElasticsearchNativeIndexFieldTypeMappingStepImpl(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(JsonObject jsonObject) {
		Gson gson = buildContext.getUserFacingGson();
		PropertyMapping mapping = gson.fromJson( jsonObject, PropertyMapping.class );
		return new ElasticsearchNativeIndexFieldTypeOptionsStepImpl( buildContext, mapping );
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(String jsonString) {
		Gson gson = buildContext.getUserFacingGson();
		return mapping( gson.fromJson( jsonString, JsonObject.class ) );
	}

}
