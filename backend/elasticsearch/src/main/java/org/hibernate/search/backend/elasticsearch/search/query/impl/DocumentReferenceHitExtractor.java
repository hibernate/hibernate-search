/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.query.spi.DocumentReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;

import com.google.gson.JsonObject;

/**
 * A hit extractor used when search results are expected to contain document references, potentially transformed.
 *
 * @see SearchQueryFactory#asReferences(SessionContext, HitAggregator)
 */
class DocumentReferenceHitExtractor implements HitExtractor<DocumentReferenceHitCollector> {

	private final DocumentReferenceExtractorHelper helper;

	DocumentReferenceHitExtractor(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public void extract(DocumentReferenceHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectReference( helper.extractDocumentReference( hit ) );
	}

}
