/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchGeoPointFieldPredicateBuilderFactory
		extends AbstractElasticsearchFieldPredicateBuilderFactory<GeoPoint> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchGeoPointFieldPredicateBuilderFactory(boolean searchable) {
		super( searchable, ElasticsearchGeoPointFieldCodec.INSTANCE );
	}

	@Override
	public MatchPredicateBuilder createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		throw log.directValueLookupNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public RangePredicateBuilder createRangePredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		throw log.rangesNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new ElasticsearchGeoPointSpatialWithinCirclePredicateBuilder( searchContext, field, codec );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new ElasticsearchGeoPointSpatialWithinPolygonPredicateBuilder( searchContext, field );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new ElasticsearchGeoPointSpatialWithinBoundingBoxPredicateBuilder( searchContext, field, codec );
	}
}
