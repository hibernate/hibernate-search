/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

import com.google.gson.JsonObject;

public class ReferenceSearchProjectionImpl implements ElasticsearchSearchProjection<Object> {

	private final DocumentReferenceExtractorHelper helper;

	ReferenceSearchProjectionImpl(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, ElasticsearchSearchQueryElementCollector elementCollector) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectReference( helper.extractDocumentReference( hit ) );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
