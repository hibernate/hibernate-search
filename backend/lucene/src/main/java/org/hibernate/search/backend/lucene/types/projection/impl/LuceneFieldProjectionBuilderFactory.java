/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface LuceneFieldProjectionBuilderFactory {

	FieldSearchProjectionBuilder<?> createFieldValueProjectionBuilder(String absoluteFieldPath);

	DistanceToFieldSearchProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath, GeoPoint center);

	boolean isDslCompatibleWith(LuceneFieldProjectionBuilderFactory other);
}
