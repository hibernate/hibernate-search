/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface LuceneFieldSortBuilderFactory {

	FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(String absoluteFieldPath);

	DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath, GeoPoint center);

	/**
	 * Determine whether another sort builder factory is DSL-compatible with this one,
	 * i.e. whether it creates builders that behave the same way.
	 *
	 * @see LuceneFieldConverter#isDslCompatibleWith(LuceneFieldConverter)
	 * @see LuceneFieldCodec#isCompatibleWith(LuceneFieldCodec)
	 *
	 * @param other Another {@link LuceneFieldSortBuilderFactory}, never {@code null}.
	 * @return {@code true} if the given predicate builder factory is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	boolean isDslCompatibleWith(LuceneFieldSortBuilderFactory other);
}
