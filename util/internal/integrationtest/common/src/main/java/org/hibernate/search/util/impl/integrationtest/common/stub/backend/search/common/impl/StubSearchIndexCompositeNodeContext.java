package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;

public interface StubSearchIndexCompositeNodeContext
		extends SearchIndexCompositeNodeContext<StubSearchIndexScope<?>>, StubSearchIndexNodeContext {

	@Override
	StubSearchIndexCompositeNodeTypeContext type();

}
