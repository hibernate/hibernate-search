/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexValueField;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexValueFieldContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

public final class StubIndexValueField<F>
		extends AbstractIndexValueField<
				StubIndexValueField<F>,
				StubSearchIndexScope,
				StubIndexValueFieldType<F>,
				StubIndexCompositeNode,
				F>
		implements StubIndexField, StubSearchIndexValueFieldContext<F> {

	private final StubIndexSchemaDataNode schemaData;

	public StubIndexValueField(StubIndexCompositeNode parent, String relativeFieldName,
			StubIndexValueFieldType<F> type, TreeNodeInclusion inclusion, boolean multiValued,
			StubIndexSchemaDataNode schemaData) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
		this.schemaData = schemaData;
	}

	@Override
	protected StubIndexValueField<F> self() {
		return this;
	}

	@Override
	public StubIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public StubIndexSchemaDataNode schemaData() {
		return schemaData;
	}
}
