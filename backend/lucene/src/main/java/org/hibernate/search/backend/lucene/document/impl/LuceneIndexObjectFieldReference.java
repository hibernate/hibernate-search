/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;


public class LuceneIndexObjectFieldReference implements IndexObjectFieldReference {

	private final ObjectFieldStorage storage;

	private LuceneIndexSchemaObjectNode schemaNode;

	public LuceneIndexObjectFieldReference(ObjectFieldStorage storage) {
		this.storage = storage;
	}

	public void enable(LuceneIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	boolean isEnabled() {
		return schemaNode != null;
	}

	LuceneIndexSchemaObjectNode getSchemaNode() {
		return schemaNode;
	}

	ObjectFieldStorage getStorage() {
		return storage;
	}
}
