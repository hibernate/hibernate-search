/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.document.model.spi.IndexNode;

public interface LuceneIndexNode
		extends IndexNode<LuceneSearchIndexScope<?>>, LuceneSearchIndexNodeContext {

	@Override
	LuceneIndexCompositeNode toComposite();

	@Override
	LuceneIndexObjectField toObjectField();

	@Override
	LuceneIndexValueField<?> toValueField();

	boolean dynamic();

}
