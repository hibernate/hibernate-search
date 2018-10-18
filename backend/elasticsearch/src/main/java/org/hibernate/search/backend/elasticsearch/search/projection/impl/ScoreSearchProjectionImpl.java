/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

import com.google.gson.JsonObject;

class ScoreSearchProjectionImpl implements ElasticsearchSearchProjection<Float> {

	private static final JsonAccessor<Boolean> TRACK_SCORES_ACCESSOR = JsonAccessor.root().property( "track_scores" )
			.asBoolean();

	ScoreSearchProjectionImpl() {
	}

	@Override
	public void contributeRequest(JsonObject requestBody, ElasticsearchSearchQueryElementCollector elementCollector) {
		TRACK_SCORES_ACCESSOR.set( requestBody, true );
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectProjection( hit.get( "_score" ).getAsFloat() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
