/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.document.Document;


class LuceneNestedObjectDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	LuceneNestedObjectDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	void contribute(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey,
			String rootId, List<Document> nestedDocuments) {
		document.add( MetadataFields.searchableMetadataField( MetadataFields.typeFieldName(), MetadataFields.TYPE_CHILD_DOCUMENT ) );
		document.add( MetadataFields.searchableMetadataField( MetadataFields.idFieldName(), rootId ) );

		document.add( MetadataFields.searchableMetadataField( MetadataFields.nestedDocumentPathFieldName(), schemaNode.getAbsolutePath() ) );

		// all the ancestors of a subdocument must be added after it
		super.contribute( multiTenancyStrategy, tenantId, routingKey, rootId, nestedDocuments );
		nestedDocuments.add( document );
	}
}
