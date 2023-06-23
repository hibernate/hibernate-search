/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.engine.backend.document.IndexFieldReference;

public class ElasticsearchIndexFieldReference<F> implements IndexFieldReference<F> {

	private ElasticsearchIndexValueField<F> schemaNode;

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[absolutePath=" + ( schemaNode == null ? null : schemaNode.absolutePath() ) + "]";
	}

	public void setSchemaNode(ElasticsearchIndexValueField<F> schemaNode) {
		this.schemaNode = schemaNode;
	}

	ElasticsearchIndexValueField<F> getSchemaNode() {
		return schemaNode;
	}
}
