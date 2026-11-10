package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;

import com.google.gson.JsonElement;

@SuppressWarnings("deprecation")
class ElasticsearchNativeIndexFieldTypeLegacyOptionsStep
		extends
		AbstractElasticsearchNativeIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeLegacyOptionsStep, JsonElement>
		implements ElasticsearchNativeIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeLegacyOptionsStep> {
	ElasticsearchNativeIndexFieldTypeLegacyOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			PropertyMapping mapping) {
		super( buildContext, mapping, GsonJsonHandlers.value() );
	}


	@Override
	protected ElasticsearchNativeIndexFieldTypeLegacyOptionsStep thisAsS() {
		return this;
	}
}
