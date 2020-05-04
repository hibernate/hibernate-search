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

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;


public class LuceneRootDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	private final MultiTenancyStrategy multiTenancyStrategy;

	LuceneRootDocumentBuilder(LuceneIndexModel model, MultiTenancyStrategy multiTenancyStrategy) {
		super( model, model.getRootNode() );
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	public LuceneIndexEntry build(String tenantId, String id, String routingKey) {
		return new LuceneIndexEntry(
				model.getIndexName(), id,
				assembleDocuments( multiTenancyStrategy, tenantId, id, routingKey )
		);
	}

	private List<Document> assembleDocuments(MultiTenancyStrategy multiTenancyStrategy,
			String tenantId, String id, String routingKey) {
		document.add( MetadataFields.searchableMetadataField( MetadataFields.typeFieldName(), MetadataFields.TYPE_MAIN_DOCUMENT ) );
		document.add( MetadataFields.searchableRetrievableMetadataField( MetadataFields.idFieldName(), id ) );

		// all the ancestors of a subdocument must be added after it
		List<Document> documents = new ArrayList<>();
		contribute( multiTenancyStrategy, tenantId, routingKey, id, documents );

		documents.add( document );

		return documents;
	}
}
