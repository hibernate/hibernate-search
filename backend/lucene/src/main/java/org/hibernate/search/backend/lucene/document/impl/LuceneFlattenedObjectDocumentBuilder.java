/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;

import org.apache.lucene.index.IndexableField;

/**
 * @author Guillaume Smet
 */
class LuceneFlattenedObjectDocumentBuilder extends AbstractLuceneDocumentBuilder {

	private final AbstractLuceneDocumentBuilder parent;

	LuceneFlattenedObjectDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode, AbstractLuceneDocumentBuilder parent) {
		super( schemaNode );
		this.parent = parent;
	}

	@Override
	public void addField(IndexableField field) {
		parent.addField( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		parent.addFieldName( absoluteFieldPath );
	}
}
