/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;


public class ElasticsearchIndexObjectFieldReference implements IndexObjectFieldReference {

	private ElasticsearchIndexSchemaObjectNode schemaNode;

	public void setSchemaNode(ElasticsearchIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	ElasticsearchIndexSchemaObjectNode getSchemaNode() {
		return schemaNode;
	}
}
