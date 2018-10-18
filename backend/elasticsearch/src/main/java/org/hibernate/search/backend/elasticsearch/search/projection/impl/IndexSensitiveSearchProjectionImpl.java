/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonObject;

class IndexSensitiveSearchProjectionImpl<T> implements ElasticsearchSearchProjection<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root()
			.property( "_index" )
			.asString();

	private final Map<String, ElasticsearchSearchProjection<T>> projectionsByIndex;

	IndexSensitiveSearchProjectionImpl(Map<String, ElasticsearchSearchProjection<T>> projectionsByIndex) {
		this.projectionsByIndex = projectionsByIndex;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, ElasticsearchSearchQueryElementCollector elementCollector) {
		for ( HitExtractor<?> extractor : projectionsByIndex.values() ) {
			extractor.contributeRequest( requestBody, elementCollector );
		}
	}

	@Override
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		String elasticsearchIndexName = HIT_INDEX_NAME_ACCESSOR.get( hit ).orElseThrow( log::elasticsearchResponseMissingData );
		projectionsByIndex.get( elasticsearchIndexName ).extract( collector, responseBody, hit );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( projectionsByIndex )
				.append( "]" );
		return sb.toString();
	}
}
