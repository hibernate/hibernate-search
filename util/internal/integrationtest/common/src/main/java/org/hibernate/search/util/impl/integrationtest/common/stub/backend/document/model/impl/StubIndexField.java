package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public interface StubIndexField
		extends IndexField<StubSearchIndexScope<?>, StubIndexCompositeNode>, StubIndexNode {
}
