/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
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
public interface LuceneFieldProjectionBuilderFactory {

	<U> FieldProjectionBuilder<U> createFieldValueProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			Class<U> expectedType, ValueConvert convert);

	DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(Set<String> indexNames, String absoluteFieldPath, GeoPoint center);

	boolean hasCompatibleCodec(LuceneFieldProjectionBuilderFactory other);

	boolean hasCompatibleConverter(LuceneFieldProjectionBuilderFactory other);

}
