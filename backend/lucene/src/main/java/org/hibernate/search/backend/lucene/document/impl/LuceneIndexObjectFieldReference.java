/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

public class LuceneIndexObjectFieldReference implements IndexObjectFieldReference {

	private LuceneIndexObjectField schemaNode;

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[absolutePath=" + ( schemaNode == null ? null : schemaNode.absolutePath() ) + "]";
	}

	public void setSchemaNode(LuceneIndexObjectField schemaNode) {
		this.schemaNode = schemaNode;
	}

	LuceneIndexObjectField getSchemaNode() {
		return schemaNode;
	}
}
