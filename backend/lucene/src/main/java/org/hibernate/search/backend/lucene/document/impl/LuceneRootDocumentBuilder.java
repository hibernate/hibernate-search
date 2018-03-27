/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;

/**
 * @author Guillaume Smet
 */
public class LuceneRootDocumentBuilder extends AbstractLuceneDocumentBuilder {

	private final Document rootDocument = new Document();

	public LuceneRootDocumentBuilder() {
		super( LuceneIndexSchemaObjectNode.root() );
	}

	@Override
	public void addField(LuceneIndexSchemaObjectNode expectedParentNode, IndexableField field) {
		checkTreeConsistency( expectedParentNode );

		rootDocument.add( field );
	}

	public LuceneIndexEntry build(String indexName, String tenantId, String id) {
		return new LuceneIndexEntry( indexName, id, assembleDocuments( indexName, tenantId, id ) );
	}

	private List<Document> assembleDocuments(String indexName, String tenantId, String id) {
		rootDocument.add( new StringField( LuceneFields.typeFieldName(), LuceneFields.TYPE_MAIN_DOCUMENT, Store.YES ) );
		rootDocument.add( new StringField( LuceneFields.indexFieldName(), indexName, Store.YES ) );
		if ( tenantId != null ) {
			rootDocument.add( new StringField( LuceneFields.tenantIdFieldName(), tenantId, Store.YES ) );
		}
		rootDocument.add( new StringField( LuceneFields.idFieldName(), id, Store.YES ) );

		List<Document> documents = new ArrayList<>();
		contribute( indexName, tenantId, id, rootDocument, documents );
		documents.add( rootDocument );

		return documents;
	}
}
