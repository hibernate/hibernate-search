/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexObjectField;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public final class LuceneIndexObjectField
		extends AbstractIndexObjectField<
				LuceneIndexObjectField,
				LuceneSearchIndexScope<?>,
				LuceneIndexCompositeNodeType,
				LuceneIndexCompositeNode,
				LuceneIndexField>
		implements LuceneIndexCompositeNode, LuceneIndexField {

	private final boolean dynamic;

	public LuceneIndexObjectField(LuceneIndexCompositeNode parent, String relativeFieldName,
			LuceneIndexCompositeNodeType type, TreeNodeInclusion inclusion, boolean multiValued,
			Map<String, LuceneIndexField> notYetInitializedStaticChildren,
			boolean dynamic) {
		super( parent, relativeFieldName, type, inclusion, multiValued, notYetInitializedStaticChildren );
		this.dynamic = dynamic;
	}

	@Override
	protected LuceneIndexObjectField self() {
		return this;
	}

	@Override
	public LuceneIndexCompositeNode toComposite() {
		return this;
	}

	@Override
	public LuceneIndexValueField<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public boolean dynamic() {
		return dynamic;
	}
}
