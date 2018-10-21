/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceToFieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.FieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class GeoPointFieldProjectionBuilderFactory implements ElasticsearchFieldProjectionBuilderFactory {

	private final ElasticsearchFieldConverter converter;

	public GeoPointFieldProjectionBuilderFactory(ElasticsearchFieldConverter converter) {
		this.converter = converter;
	}

	@Override
	public FieldSearchProjectionBuilder<?> createFieldValueProjectionBuilder(String absoluteFieldPath) {
		return new FieldSearchProjectionBuilderImpl<>( absoluteFieldPath, converter );
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
			GeoPoint center) {
		return new DistanceToFieldSearchProjectionBuilderImpl( absoluteFieldPath, center );
	}

	@Override
	public boolean isDslCompatibleWith(ElasticsearchFieldProjectionBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !( obj instanceof GeoPointFieldProjectionBuilderFactory ) ) {
			return false;
		}

		GeoPointFieldProjectionBuilderFactory other = (GeoPointFieldProjectionBuilderFactory) obj;

		return converter.isDslCompatibleWith( other.converter );
	}
}
