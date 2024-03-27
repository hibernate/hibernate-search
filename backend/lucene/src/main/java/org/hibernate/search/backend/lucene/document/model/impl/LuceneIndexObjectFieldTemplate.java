/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public class LuceneIndexObjectFieldTemplate
		extends AbstractLuceneIndexFieldTemplate<LuceneIndexCompositeNodeType> {

	public LuceneIndexObjectFieldTemplate(LuceneIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, LuceneIndexCompositeNodeType type, TreeNodeInclusion inclusion,
			boolean multiValued) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

	@Override
	protected LuceneIndexField createNode(LuceneIndexCompositeNode parent, String relativePath,
			LuceneIndexCompositeNodeType type, TreeNodeInclusion inclusion, boolean multiValued) {
		return new LuceneIndexObjectField( parent, relativePath, type, inclusion, multiValued,
				Collections.emptyMap(), true );
	}

}
