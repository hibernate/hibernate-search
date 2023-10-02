/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

public class ElasticsearchIndexObjectFieldReference implements IndexObjectFieldReference {

	private ElasticsearchIndexObjectField schemaNode;

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[absolutePath=" + ( schemaNode == null ? null : schemaNode.absolutePath() ) + "]";
	}

	public void setSchemaNode(ElasticsearchIndexObjectField schemaNode) {
		this.schemaNode = schemaNode;
	}

	ElasticsearchIndexObjectField getSchemaNode() {
		return schemaNode;
	}
}
