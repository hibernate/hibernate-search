package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;

public interface StubSearchIndexNodeContext
		extends SearchIndexNodeContext<StubSearchIndexScope<?>> {

	@Override
	StubSearchIndexCompositeNodeContext toComposite();

	@Override
	StubSearchIndexValueFieldContext<?> toValueField();

}
