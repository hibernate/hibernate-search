/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;

import com.google.gson.JsonObject;

/**
 * A hit extractor used when projecting on the document reference, when we don't want the reference to be transformed,
 * but we just want the raw reference to be inserted into the projection.
 *
 * @see SearchQueryFactory#asProjections(org.hibernate.search.engine.common.spi.SessionContext,
 * org.hibernate.search.engine.search.query.spi.HitAggregator, org.hibernate.search.engine.search.SearchProjection...)
 */
class DocumentReferenceProjectionHitExtractor implements HitExtractor<ProjectionHitCollector> {

	private final DocumentReferenceExtractorHelper helper;

	DocumentReferenceProjectionHitExtractor(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectProjection( helper.extractDocumentReference( hit ) );
	}

}
