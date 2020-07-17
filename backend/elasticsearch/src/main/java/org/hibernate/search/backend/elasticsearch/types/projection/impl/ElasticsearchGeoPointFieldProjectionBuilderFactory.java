/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchDistanceToFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class ElasticsearchGeoPointFieldProjectionBuilderFactory
		extends ElasticsearchStandardFieldProjectionBuilderFactory<GeoPoint> {

	public ElasticsearchGeoPointFieldProjectionBuilderFactory(boolean projectable,
			ElasticsearchFieldCodec<GeoPoint> codec) {
		super( projectable, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<GeoPoint> field, GeoPoint center) {
		checkProjectable( field );

		return new ElasticsearchDistanceToFieldProjection.Builder( searchContext, field, center );
	}
}
