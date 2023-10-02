/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;

import org.apache.lucene.document.Document;

public class LuceneRootDocumentBuilder extends AbstractLuceneDocumentElementBuilder {

	private final MultiTenancyStrategy multiTenancyStrategy;

	LuceneRootDocumentBuilder(LuceneIndexModel model, MultiTenancyStrategy multiTenancyStrategy) {
		super( model, model.root(), new LuceneDocumentContentImpl() );
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	public LuceneIndexEntry build(String tenantId, String id, String routingKey) {
		return new LuceneIndexEntry(
				model.hibernateSearchName(), id,
				assembleDocuments( multiTenancyStrategy, tenantId, id, routingKey )
		);
	}

	@Override
	void ensureDynamicValueDetectedByExistsPredicateOnObjectField() {
		// This is not an object field: nothing to do.
	}

	private List<Document> assembleDocuments(MultiTenancyStrategy multiTenancyStrategy,
			String tenantId, String id, String routingKey) {
		// We own the document content, so we finalize it ourselves.
		Document document = documentContent.finalizeDocument( multiTenancyStrategy, tenantId, routingKey );
		document.add(
				MetadataFields.searchableMetadataField( MetadataFields.typeFieldName(), MetadataFields.TYPE_MAIN_DOCUMENT ) );
		document.add( MetadataFields.searchableMetadataField( MetadataFields.idFieldName(), id ) );
		document.add( MetadataFields.retrievableMetadataField( MetadataFields.idDocValueFieldName(), id ) );

		// In the list of documents, a child must appear before its parent,
		// so we let children contribute their document first.
		List<Document> documents = new ArrayList<>();
		contribute( multiTenancyStrategy, tenantId, routingKey, id, documents );
		documents.add( document );

		return documents;
	}
}
