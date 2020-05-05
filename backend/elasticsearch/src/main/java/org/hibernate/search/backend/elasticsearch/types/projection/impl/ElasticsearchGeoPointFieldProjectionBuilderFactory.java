/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchDistanceToFieldProjectionBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class ElasticsearchGeoPointFieldProjectionBuilderFactory
		extends ElasticsearchStandardFieldProjectionBuilderFactory<GeoPoint> {

	public ElasticsearchGeoPointFieldProjectionBuilderFactory(boolean projectable,
			ProjectionConverter<? super GeoPoint, ?> converter, ProjectionConverter<? super GeoPoint, GeoPoint> rawConverter,
			ElasticsearchFieldCodec<GeoPoint> codec) {
		super( projectable, converter, rawConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(Set<String> indexNames, String absoluteFieldPath, String nestedPath,
			GeoPoint center) {
		checkProjectable( absoluteFieldPath, projectable );

		return new ElasticsearchDistanceToFieldProjectionBuilder( indexNames, absoluteFieldPath, nestedPath, center );
	}
}
