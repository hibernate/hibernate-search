/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchDistanceToFieldProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.SortTypeKeys;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchGeoPointFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchDistanceSort;
import org.hibernate.search.engine.spatial.GeoPoint;


class ElasticsearchGeoPointIndexFieldTypeOptionsStep
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<ElasticsearchGeoPointIndexFieldTypeOptionsStep, GeoPoint> {

	ElasticsearchGeoPointIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, GeoPoint.class, DataTypes.GEO_POINT );
	}

	@Override
	protected void complete() {
		ElasticsearchGeoPointFieldCodec codec = ElasticsearchGeoPointFieldCodec.INSTANCE;
		builder.codec( codec );

		// We need doc values for the projection script when not sorting on the same field
		builder.mapping().setDocValues( resolvedSortable || resolvedProjectable );

		builder.predicateBuilderFactory(
				new ElasticsearchGeoPointFieldPredicateBuilderFactory( resolvedSearchable ) );

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.DISTANCE, new ElasticsearchDistanceSort.Factory( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
			builder.queryElementFactory( ProjectionTypeKeys.DISTANCE,
					new ElasticsearchDistanceToFieldProjection.Factory( codec ) );
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
