/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaFieldNode {

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final ElasticsearchFieldCodec codec;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, ElasticsearchFieldCodec codec) {
		this.parent = parent;
		this.codec = codec;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[parent=" + parent + "]";
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public ElasticsearchFieldCodec getCodec() {
		return codec;
	}
}
