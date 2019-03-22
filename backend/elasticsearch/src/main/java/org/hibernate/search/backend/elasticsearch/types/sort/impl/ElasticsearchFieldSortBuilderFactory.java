/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchConverterCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A field-scoped factory for search sort builders.
 * <p>
 * allowing fine-grained control over the type of sort created for each field.
 * <p>
 * For example, a sort on an {@link Integer} field
 * will not have its {@link FieldSortBuilder#missingAs(Object, DslConverter)} method
 * accept the same arguments as a sort on a {@link java.time.LocalDate} field;
 * having a separate {@link ElasticsearchFieldSortBuilderFactory} for those two fields
 * allows to implement the different behavior.
 * <p>
 * Similarly, and perhaps more importantly,
 * having a per-field factory allows us to throw detailed exceptions
 * when users try to create a sort that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface ElasticsearchFieldSortBuilderFactory {

	FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, ElasticsearchConverterCompatibilityChecker converterChecker);

	DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center);

	/**
	 * Determine whether another sort builder factory is DSL-compatible with this one,
	 * i.e. whether it creates builders that behave the same way.
	 *
	 * @see ToDocumentFieldValueConverter#isCompatibleWith(ToDocumentFieldValueConverter)
	 * @see org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec#isCompatibleWith(ElasticsearchFieldCodec)
	 * *
	 * @param other Another {@link ElasticsearchFieldSortBuilderFactory}, never {@code null}.
	 * @param dslConverter whether {@code ENABLED}, converters will also be taken in account for the matching
	 * @return {@code true} if the given sort builder factory is DSL-compatible.
	 * {@code false} otherwise, or when in doubt.
	 */
	default boolean isDslCompatibleWith(ElasticsearchFieldSortBuilderFactory other, DslConverter dslConverter) {
		if ( !hasCompatibleCodec( other ) ) {
			return false;
		}

		return ( !dslConverter.isEnabled() || hasCompatibleConverter( other ) );
	}

	boolean hasCompatibleCodec(ElasticsearchFieldSortBuilderFactory other);

	boolean hasCompatibleConverter(ElasticsearchFieldSortBuilderFactory other);
}
