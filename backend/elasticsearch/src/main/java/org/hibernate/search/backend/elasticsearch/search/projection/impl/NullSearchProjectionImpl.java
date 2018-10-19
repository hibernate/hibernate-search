/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

import com.google.gson.JsonObject;

class NullSearchProjectionImpl<T> implements ElasticsearchSearchProjection<T> {

	@SuppressWarnings("rawtypes")
	private static final NullSearchProjectionImpl INSTANCE = new NullSearchProjectionImpl();

	@SuppressWarnings("unchecked")
	public static <U> NullSearchProjectionImpl<U> get() {
		return (NullSearchProjectionImpl<U>) INSTANCE;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		// Nothing to contribute
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		collector.collectProjection( null );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
