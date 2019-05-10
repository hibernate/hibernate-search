/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;

/**
 * @author Guillaume Smet
 */
class LuceneNestedObjectDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	LuceneNestedObjectDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId, Document currentDocument,
			List<Document> nestedDocuments) {
		document.add( new StringField( LuceneFields.typeFieldName(), LuceneFields.TYPE_CHILD_DOCUMENT, Store.YES ) );
		document.add( new StringField( LuceneFields.rootIndexFieldName(), rootIndexName, Store.YES ) );
		document.add( new StringField( LuceneFields.rootIdFieldName(), rootId, Store.YES ) );
		document.add( new StringField( LuceneFields.nestedDocumentPathFieldName(), schemaNode.getAbsolutePath(), Store.YES ) );

		// all the ancestors of a subdocument must be added after it
		super.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, document, nestedDocuments );
		nestedDocuments.add( document );
	}
}
