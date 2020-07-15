/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface ElasticsearchSearchFieldTypeContext<F> {

	DslConverter<?, F> dslConverter();

	DslConverter<F, F> rawDslConverter();

	default DslConverter<?, F> dslConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawDslConverter();
			case YES:
			default:
				return dslConverter();
		}
	}

	ProjectionConverter<F, ?> projectionConverter();

	ProjectionConverter<F, F> rawProjectionConverter();

	default ProjectionConverter<F, ?> projectionConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawProjectionConverter();
			case YES:
			default:
				return projectionConverter();
		}
	}

	Optional<String> searchAnalyzerName();

	Optional<String> normalizerName();

	ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory();

	ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory();

	ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory();

	ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory();

}
