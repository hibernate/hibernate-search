/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A field-scoped factory for search sort builders.
 * <p>
 * Implementations are created and stored for each field at bootstrap,
 * allowing fine-grained control over the type of sort created for each field.
 * <p>
 * For example, a sort on an {@link Integer} field
 * will not have its {@link FieldSortBuilder#missingAs(Object, ValueConvert)} method
 * accept the same arguments as a sort on a {@link java.time.LocalDate} field;
 * having a separate {@link LuceneFieldSortBuilderFactory} for those two fields
 * allows to implement the different behavior.
 * <p>
 * Similarly, and perhaps more importantly,
 * having a per-field factory allows us to throw detailed exceptions
 * when users try to create a sort that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface LuceneFieldSortBuilderFactory<F> {

	boolean isSortable();

	boolean isCompatibleWith(LuceneFieldSortBuilderFactory<?> other);

	FieldSortBuilder createFieldSortBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field);

	DistanceSortBuilder createDistanceSortBuilder(
			LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field,
			GeoPoint center);

}
