/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldContext;

public interface StubSearchIndexValueFieldContext<F>
		extends SearchIndexValueFieldContext<StubSearchIndexScope>, StubSearchIndexNodeContext {

	@Override
	StubSearchIndexValueFieldTypeContext<F> type();

}
