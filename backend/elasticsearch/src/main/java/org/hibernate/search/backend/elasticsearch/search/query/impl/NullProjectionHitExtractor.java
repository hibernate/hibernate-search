/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

import com.google.gson.JsonObject;

class NullProjectionHitExtractor implements HitExtractor<ProjectionHitCollector> {

	private static final NullProjectionHitExtractor INSTANCE = new NullProjectionHitExtractor();

	public static NullProjectionHitExtractor get() {
		return INSTANCE;
	}

	private NullProjectionHitExtractor() {
		// Private constructor, use get() instead
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		// Nothing to do
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectProjection( null );
	}
}
