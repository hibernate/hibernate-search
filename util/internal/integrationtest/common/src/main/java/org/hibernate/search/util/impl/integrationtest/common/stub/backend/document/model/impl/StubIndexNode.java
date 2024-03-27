/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public interface StubIndexNode
		extends IndexNode<StubSearchIndexScope>, StubSearchIndexNodeContext {

	@Override
	StubIndexCompositeNode toComposite();

	@Override
	StubIndexObjectField toObjectField();

	@Override
	StubIndexValueField<?> toValueField();

	StubIndexSchemaDataNode schemaData();

}
