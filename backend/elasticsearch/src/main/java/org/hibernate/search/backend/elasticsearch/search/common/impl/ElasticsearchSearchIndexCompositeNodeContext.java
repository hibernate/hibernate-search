/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.Map;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;

public interface ElasticsearchSearchIndexCompositeNodeContext
		extends SearchIndexCompositeNodeContext<
				ElasticsearchSearchIndexScope<?>>,
		ElasticsearchSearchIndexNodeContext {

	@Override
	ElasticsearchSearchIndexCompositeNodeTypeContext type();

	@Override
	Map<String, ? extends ElasticsearchSearchIndexNodeContext> staticChildrenByName();

}
