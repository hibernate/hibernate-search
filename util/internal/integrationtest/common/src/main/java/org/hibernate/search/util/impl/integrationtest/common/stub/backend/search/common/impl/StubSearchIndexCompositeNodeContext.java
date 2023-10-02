/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;

public interface StubSearchIndexCompositeNodeContext
		extends SearchIndexCompositeNodeContext<StubSearchIndexScope>, StubSearchIndexNodeContext {

	@Override
	StubSearchIndexCompositeNodeTypeContext type();

}
