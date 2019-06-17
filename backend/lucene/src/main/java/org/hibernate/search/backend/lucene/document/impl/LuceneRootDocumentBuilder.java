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
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;


public class LuceneRootDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	public LuceneRootDocumentBuilder() {
		super( LuceneIndexSchemaObjectNode.root() );
	}

	public LuceneIndexEntry build(String indexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String id) {
		return new LuceneIndexEntry( indexName, id, assembleDocuments( indexName, multiTenancyStrategy, tenantId, id ) );
	}

	private List<Document> assembleDocuments(String indexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String id) {
		document.add( new StringField( LuceneFields.typeFieldName(), LuceneFields.TYPE_MAIN_DOCUMENT, Store.YES ) );
		document.add( new StringField( LuceneFields.indexFieldName(), indexName, Store.YES ) );
		document.add( new StringField( LuceneFields.idFieldName(), id, Store.YES ) );

		// all the ancestors of a subdocument must be added after it
		List<Document> documents = new ArrayList<>();
		contribute( indexName, multiTenancyStrategy, tenantId, id, document, documents );
		documents.add( document );

		return documents;
	}
}
