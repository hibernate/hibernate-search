/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchDistanceToFieldProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointSpatialWithinCirclePredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointSpatialWithinPolygonPredicate;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchDistanceSort;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.engine.spatial.GeoPoint;

class ElasticsearchGeoPointIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<ElasticsearchGeoPointIndexFieldTypeOptionsStep, GeoPoint> {

	ElasticsearchGeoPointIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, GeoPoint.class, DataTypes.GEO_POINT );
	}

	@Override
	protected void complete() {
		ElasticsearchGeoPointFieldCodec codec = ElasticsearchGeoPointFieldCodec.INSTANCE;
		builder.codec( codec );

		// Since docs values are going to be available as soon as the filed is either sortable or projectable
		// it would open the other capability automatically. Hence:
		resolvedSortable = resolvedSortable || resolvedProjectable;
		resolvedProjectable = resolvedSortable;

		// We need doc values for the projection script when not sorting on the same field
		builder.mapping().setDocValues( resolvedProjectable );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE,
					new ElasticsearchGeoPointSpatialWithinCirclePredicate.Factory( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_POLYGON,
					new ElasticsearchGeoPointSpatialWithinPolygonPredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX,
					new ElasticsearchGeoPointSpatialWithinBoundingBoxPredicate.Factory( codec ) );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.DISTANCE, new ElasticsearchDistanceSort.Factory() );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
			builder.queryElementFactory( ProjectionTypeKeys.DISTANCE,
					new ElasticsearchDistanceToFieldProjection.Factory() );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			// No supported aggregation at the moment.
		}
	}

	@Override
	protected ElasticsearchGeoPointIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
