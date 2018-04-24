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
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;

/**
 * @author Guillaume Smet
 */
public class LuceneRootDocumentBuilder extends AbstractLuceneDocumentBuilder {

	private final Document rootDocument = new Document();

	public LuceneRootDocumentBuilder() {
		super( LuceneIndexSchemaObjectNode.root() );
	}

	@Override
	public void addField(IndexableField field) {
		rootDocument.add( field );
	}

	public LuceneIndexEntry build(String indexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String id) {
		return new LuceneIndexEntry( indexName, id, assembleDocuments( indexName, multiTenancyStrategy, tenantId, id ) );
	}

	private List<Document> assembleDocuments(String indexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String id) {
		rootDocument.add( new StringField( LuceneFields.typeFieldName(), LuceneFields.TYPE_MAIN_DOCUMENT, Store.YES ) );
		rootDocument.add( new StringField( LuceneFields.indexFieldName(), indexName, Store.YES ) );
		rootDocument.add( new StringField( LuceneFields.idFieldName(), id, Store.YES ) );

		multiTenancyStrategy.contributeToIndexedDocument( rootDocument, tenantId );

		List<Document> documents = new ArrayList<>();
		contribute( indexName, multiTenancyStrategy, tenantId, id, rootDocument, documents );
		documents.add( rootDocument );

		return documents;
	}
}
