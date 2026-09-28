package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;

public interface ElasticsearchSearchProjectionIndexScope<S extends ElasticsearchSearchProjectionIndexScope<?>>
		extends SearchProjectionIndexScope<S> {

	@Override
	ElasticsearchSearchProjectionBuilderFactory projectionBuilders();

}
