/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;
import org.hibernate.search.engine.backend.document.IndexFieldReference;

public class LuceneIndexFieldReference<F> implements IndexFieldReference<F> {

	private LuceneIndexValueField<F> schemaNode;

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[absolutePath=" + ( schemaNode == null ? null : schemaNode.absolutePath() ) + "]";
	}

	public void setSchemaNode(LuceneIndexValueField<F> schemaNode) {
		this.schemaNode = schemaNode;
	}

	LuceneIndexValueField<F> getSchemaNode() {
		return schemaNode;
	}
}
