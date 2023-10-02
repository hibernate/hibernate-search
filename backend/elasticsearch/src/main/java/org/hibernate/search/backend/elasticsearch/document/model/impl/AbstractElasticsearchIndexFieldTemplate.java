/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexFieldTemplate;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public abstract class AbstractElasticsearchIndexFieldTemplate<FT>
		extends AbstractIndexFieldTemplate<
				ElasticsearchIndexModel,
				ElasticsearchIndexField,
				ElasticsearchIndexCompositeNode,
				FT> {

	AbstractElasticsearchIndexFieldTemplate(ElasticsearchIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, FT type, TreeNodeInclusion inclusion, boolean multiValued) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

}
