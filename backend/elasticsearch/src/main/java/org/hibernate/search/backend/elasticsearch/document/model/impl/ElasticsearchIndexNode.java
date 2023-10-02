/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.document.model.spi.IndexNode;

public interface ElasticsearchIndexNode
		extends IndexNode<ElasticsearchSearchIndexScope<?>>, ElasticsearchSearchIndexNodeContext {

	@Override
	ElasticsearchIndexCompositeNode toComposite();

	@Override
	ElasticsearchIndexObjectField toObjectField();

	@Override
	ElasticsearchIndexValueField<?> toValueField();

}
