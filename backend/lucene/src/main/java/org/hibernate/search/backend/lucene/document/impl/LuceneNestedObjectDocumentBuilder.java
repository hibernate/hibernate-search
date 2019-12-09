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
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.document.Document;


class LuceneNestedObjectDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	LuceneNestedObjectDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId,
			List<Document> nestedDocuments) {
		document.add( LuceneFields.searchableMetadataField( LuceneFields.typeFieldName(), LuceneFields.TYPE_CHILD_DOCUMENT ) );
		document.add( LuceneFields.searchableMetadataField( LuceneFields.rootIndexFieldName(), rootIndexName ) );
		document.add( LuceneFields.retrievableMetadataField( LuceneFields.rootIdFieldName(), rootId ) );
		document.add( LuceneFields.searchableMetadataField( LuceneFields.nestedDocumentPathFieldName(), schemaNode.getAbsolutePath() ) );

		// all the ancestors of a subdocument must be added after it
		super.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, nestedDocuments );
		nestedDocuments.add( document );
	}
}
