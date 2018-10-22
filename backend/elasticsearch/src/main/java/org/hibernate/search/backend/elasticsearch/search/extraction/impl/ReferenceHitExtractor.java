/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.extraction.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExecutionContext;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

import com.google.gson.JsonObject;

/**
 * A hit extractor used when search results are expected to contain document references, potentially transformed.
 *
 * @see SearchQueryBuilderFactory#asReferences(SessionContextImplementor, HitAggregator)
 */
public class ReferenceHitExtractor implements HitExtractor<ReferenceHitCollector> {

	private final DocumentReferenceExtractorHelper helper;

	public ReferenceHitExtractor(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public void extract(ReferenceHitCollector collector, JsonObject responseBody, JsonObject hit, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		collector.collectReference( helper.extractDocumentReference( hit ) );
	}

}
