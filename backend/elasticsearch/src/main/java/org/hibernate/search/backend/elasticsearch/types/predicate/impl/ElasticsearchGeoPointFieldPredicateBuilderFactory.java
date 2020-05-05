/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
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
	public boolean hasCompatibleConverter(ElasticsearchFieldPredicateBuilderFactory other) {
		return getClass().equals( other.getClass() );
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			ElasticsearchCompatibilityChecker converterChecker, ElasticsearchCompatibilityChecker analyzerChecker) {
		throw log.directValueLookupNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> createRangePredicateBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			ElasticsearchCompatibilityChecker converterChecker) {
		throw log.rangesNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchGeoPointSpatialWithinCirclePredicateBuilder( absoluteFieldPath, nestedPathHierarchy, codec );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchGeoPointSpatialWithinPolygonPredicateBuilder( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		checkSearchable( absoluteFieldPath );
		return new ElasticsearchGeoPointSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath, nestedPathHierarchy, codec );
	}
}
