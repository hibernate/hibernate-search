package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;

public interface LuceneSearchIndexNodeContext
		extends SearchIndexNodeContext<LuceneSearchIndexScope<?>> {

	@Override
	LuceneSearchIndexCompositeNodeContext toComposite();

	@Override
	LuceneSearchIndexCompositeNodeContext toObjectField();

	@Override
	LuceneSearchIndexValueFieldContext<?> toValueField();

}
