/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.DistanceToFieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.FieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class GeoPointFieldProjectionBuilderFactory<T> implements LuceneFieldProjectionBuilderFactory {

	private final LuceneFieldCodec<T> codec;

	private final LuceneFieldConverter<T, ?> converter;

	public GeoPointFieldProjectionBuilderFactory(LuceneFieldCodec<T> codec, LuceneFieldConverter<T, ?> converter) {
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public FieldSearchProjectionBuilder<?> createFieldValueProjectionBuilder(String absoluteFieldPath) {
		return new FieldSearchProjectionBuilderImpl<>( absoluteFieldPath, codec, converter );
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
			GeoPoint center) {
		return new DistanceToFieldSearchProjectionBuilderImpl( absoluteFieldPath, center );
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldProjectionBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != GeoPointFieldProjectionBuilderFactory.class ) {
			return false;
		}

		GeoPointFieldProjectionBuilderFactory<?> other = (GeoPointFieldProjectionBuilderFactory<?>) obj;

		return codec.isCompatibleWith( other.codec ) &&
				converter.isDslCompatibleWith( other.converter );
	}
}
