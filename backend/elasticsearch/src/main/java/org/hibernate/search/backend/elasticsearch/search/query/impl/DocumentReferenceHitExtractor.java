/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
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
	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root().property( "_index" ).asString();
	private static final JsonAccessor<String> HIT_ID_ACCESSOR = JsonAccessor.root().property( "_id" ).asString();

	private static final DocumentReferenceHitExtractor INSTANCE = new DocumentReferenceHitExtractor();

	static DocumentReferenceHitExtractor get() {
		return INSTANCE;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		// Nothing to do
	}

	@Override
	public void extract(DocumentReferenceHitCollector collector, JsonObject responseBody, JsonObject hit) {
		String indexName = HIT_INDEX_NAME_ACCESSOR.get( hit ).get();
		String id = HIT_ID_ACCESSOR.get( hit ).get();
		DocumentReference documentReference = new ElasticsearchDocumentReference( indexName, id );
		collector.collectReference( documentReference );
	}

}
