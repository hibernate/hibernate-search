/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;


public class LuceneIndexObjectFieldReference implements IndexObjectFieldReference {

	private LuceneIndexSchemaObjectNode schemaNode;

	public void setSchemaNode(LuceneIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	LuceneIndexSchemaObjectNode getSchemaNode() {
		return schemaNode;
	}
}
