/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;

import org.apache.lucene.analysis.Analyzer;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface LuceneSearchFieldTypeContext<F> {

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

	Analyzer searchAnalyzerOrNormalizer();

	LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory();

	LuceneFieldSortBuilderFactory<F> sortBuilderFactory();

	LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory();

	LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory();
}
