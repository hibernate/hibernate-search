package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.util.json.JsonHandler;

class ElasticsearchNativeIndexFieldTypeHandlerOptionsStep<T>
		extends
		AbstractElasticsearchNativeIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeHandlerOptionsStep<T>, T> {
	ElasticsearchNativeIndexFieldTypeHandlerOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			PropertyMapping mapping, JsonHandler<T> handler) {
		super( buildContext, mapping, handler );
	}

	@Override
	protected ElasticsearchNativeIndexFieldTypeHandlerOptionsStep<T> thisAsS() {
		return this;
	}
}
