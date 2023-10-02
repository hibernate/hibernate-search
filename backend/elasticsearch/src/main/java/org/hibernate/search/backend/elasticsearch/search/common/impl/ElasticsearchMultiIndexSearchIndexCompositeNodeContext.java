/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public final class ElasticsearchMultiIndexSearchIndexCompositeNodeContext
		extends AbstractMultiIndexSearchIndexCompositeNodeContext<
				ElasticsearchSearchIndexCompositeNodeContext,
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchSearchIndexCompositeNodeTypeContext,
				ElasticsearchSearchIndexNodeContext>
		implements ElasticsearchSearchIndexCompositeNodeContext,
		ElasticsearchSearchIndexCompositeNodeTypeContext {

	public ElasticsearchMultiIndexSearchIndexCompositeNodeContext(ElasticsearchSearchIndexScope<?> scope,
			String absolutePath,
			List<? extends ElasticsearchSearchIndexCompositeNodeContext> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeContext self() {
		return this;
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeTypeContext selfAsNodeType() {
		return this;
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeTypeContext typeOf(
			ElasticsearchSearchIndexCompositeNodeContext indexElement) {
		return indexElement.type();
	}

	@Override
	public ElasticsearchSearchIndexValueFieldContext<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	protected ElasticsearchSearchIndexNodeContext childInScope(String childRelativeName) {
		return scope.child( this, childRelativeName );
	}
}
