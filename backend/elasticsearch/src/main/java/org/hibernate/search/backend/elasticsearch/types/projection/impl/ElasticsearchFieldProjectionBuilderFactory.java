/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.projection.impl;

import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A field-scoped factory for search projection builders.
 * <p>
 * Implementations are created and stored for each field at bootstrap,
 * allowing fine-grained control over the type of projection created for each field.
 * <p>
 * Having a per-field factory allows us to throw detailed exceptions
 * when users try to create a projection that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface ElasticsearchFieldProjectionBuilderFactory {

	<T> FieldSearchProjectionBuilder<T> createFieldValueProjectionBuilder(String absoluteFieldPath,
			Class<T> expectedType);

	DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath, GeoPoint center);

	/**
	 * Determine whether another projection builder factory is DSL-compatible with this one,
	 * i.e. whether it creates builders that behave the same way.
	 *
	 * @see ElasticsearchFieldConverter#isConvertFromDslCompatibleWith(ElasticsearchFieldConverter)
	 *
	 * @param other Another {@link ElasticsearchFieldProjectionBuilderFactory}, never {@code null}.
	 * @return {@code true} if the given predicate builder factory is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isDslCompatibleWith(ElasticsearchFieldProjectionBuilderFactory other);
}
