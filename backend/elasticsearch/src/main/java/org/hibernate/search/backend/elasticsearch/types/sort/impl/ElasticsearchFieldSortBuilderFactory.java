/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface ElasticsearchFieldSortBuilderFactory {

	FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath);

	DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center);

	/**
	 * Determine whether another sort builder factory is DSL-compatible with this one,
	 * i.e. whether it creates builders that behave the same way.
	 *
	 * @see ElasticsearchFieldConverter#isDslCompatibleWith(ElasticsearchFieldConverter)
	 *
	 * @param other Another {@link ElasticsearchFieldSortBuilderFactory}, never {@code null}.
	 * @return {@code true} if the given sort builder factory is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isDslCompatibleWith(ElasticsearchFieldSortBuilderFactory other);
}
