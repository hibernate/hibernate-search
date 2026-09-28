package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;

public interface ElasticsearchSearchIndexNodeContext
		extends SearchIndexNodeContext<ElasticsearchSearchIndexScope<?>> {

	@Override
	ElasticsearchSearchIndexCompositeNodeContext toComposite();

	@Override
	ElasticsearchSearchIndexCompositeNodeContext toObjectField();

	@Override
	ElasticsearchSearchIndexValueFieldContext<?> toValueField();

}
