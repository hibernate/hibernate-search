/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A field-scoped factory for search sort builders.
 * <p>
 * allowing fine-grained control over the type of sort created for each field.
 * <p>
 * For example, a sort on an {@link Integer} field
 * will not have its {@link FieldSortBuilder#missingAs(Object, ValueConvert)} method
 * accept the same arguments as a sort on a {@link java.time.LocalDate} field;
 * having a separate {@link ElasticsearchFieldSortBuilderFactory} for those two fields
 * allows to implement the different behavior.
 * <p>
 * Similarly, and perhaps more importantly,
 * having a per-field factory allows us to throw detailed exceptions
 * when users try to create a sort that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface ElasticsearchFieldSortBuilderFactory<F> {

	boolean isSortable();

	boolean isCompatibleWith(ElasticsearchFieldSortBuilderFactory<?> other);

	FieldSortBuilder createFieldSortBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field);

	DistanceSortBuilder createDistanceSortBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, GeoPoint center);

}
