/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.document.Document;


class LuceneNestedObjectFieldBuilder extends AbstractLuceneObjectFieldBuilder {

	LuceneNestedObjectFieldBuilder(LuceneIndexModel model, LuceneIndexObjectField schemaNode,
			AbstractLuceneDocumentElementBuilder parent) {
		super( model, schemaNode, parent, new LuceneDocumentContentImpl() );
	}

	@Override
	void contribute(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey,
			String rootId, List<Document> nestedDocuments) {

		// We own the document content, so we finalize it ourselves.
		Document document = documentContent.finalizeDocument( multiTenancyStrategy, tenantId, routingKey );
		document.add( MetadataFields.searchableMetadataField( MetadataFields.typeFieldName(), MetadataFields.TYPE_CHILD_DOCUMENT ) );
		document.add( MetadataFields.searchableMetadataField( MetadataFields.idFieldName(), rootId ) );
		document.add( MetadataFields.searchableMetadataField( MetadataFields.nestedDocumentPathFieldName(), schemaNode.absolutePath() ) );

		// In the list of documents, a child must appear before its parent,
		// so we let children contribute their document first.
		super.contribute( multiTenancyStrategy, tenantId, routingKey, rootId, nestedDocuments );
		nestedDocuments.add( document );
	}
}
