package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeTypeContext;

public interface ElasticsearchSearchIndexCompositeNodeTypeContext
		extends SearchIndexCompositeNodeTypeContext<
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchSearchIndexCompositeNodeContext> {

}
