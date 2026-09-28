package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;

public interface StubSearchIndexValueFieldTypeContext<F>
		extends SearchIndexValueFieldTypeContext<StubSearchIndexScope<?>, StubSearchIndexValueFieldContext<F>, F> {

}
