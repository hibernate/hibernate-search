/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.FieldSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class StandardFieldProjectionBuilderFactory implements ElasticsearchFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchFieldConverter converter;

	public StandardFieldProjectionBuilderFactory(ElasticsearchFieldConverter converter) {
		this.converter = converter;
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> createFieldValueProjectionBuilder(String absoluteFieldPath,
			Class<T> expectedType) {
		if ( !converter.isProjectionCompatibleWith( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return new FieldSearchProjectionBuilderImpl<>( absoluteFieldPath, converter );
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean isDslCompatibleWith(ElasticsearchFieldProjectionBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != StandardFieldProjectionBuilderFactory.class ) {
			return false;
		}

		StandardFieldProjectionBuilderFactory other = (StandardFieldProjectionBuilderFactory) obj;

		return converter.isDslCompatibleWith( other.converter );
	}
}
