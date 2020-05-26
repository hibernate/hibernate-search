/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.engine.backend.document.IndexFieldReference;


public class ElasticsearchIndexFieldReference<F> implements IndexFieldReference<F> {

	private ElasticsearchIndexSchemaFieldNode<F> schemaNode;

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[absolutePath=" + ( schemaNode == null ? null : schemaNode.absolutePath() ) + "]";
	}

	public void setSchemaNode(ElasticsearchIndexSchemaFieldNode<F> schemaNode) {
		this.schemaNode = schemaNode;
	}

	ElasticsearchIndexSchemaFieldNode<F> getSchemaNode() {
		return schemaNode;
	}
}
