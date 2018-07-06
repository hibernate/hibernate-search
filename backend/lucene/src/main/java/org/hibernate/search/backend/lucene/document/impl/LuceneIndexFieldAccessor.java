/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;


/**
 * @author Guillaume Smet
 */
public class LuceneIndexFieldAccessor<T> implements IndexFieldAccessor<T> {

	private final LuceneIndexSchemaFieldNode<T> schemaNode;

	public LuceneIndexFieldAccessor(LuceneIndexSchemaFieldNode<T> schemaNode) {
		this.schemaNode = schemaNode;
	}

	@Override
	public void write(DocumentElement target, T value) {
		LuceneDocumentBuilder documentBuilder = (LuceneDocumentBuilder) target;
		documentBuilder.checkTreeConsistency( schemaNode.getParent() );
		schemaNode.getCodec().encode( documentBuilder, schemaNode.getAbsoluteFieldPath(), value );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[schemaNode=" + schemaNode + "]";
	}
}
