/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class ElasticsearchIndexFieldType<F> implements IndexValueFieldTypeDescriptor, IndexFieldType<F> {
	private final Class<F> valueType;
	private final DslConverter<?, ? extends F> dslConverter;
	private final ProjectionConverter<? super F, ?> projectionConverter;
	private final ElasticsearchFieldCodec<F> codec;
	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;
	private final ElasticsearchFieldSortBuilderFactory sortBuilderFactory;
	private final ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory;
	private final ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory;
	private final PropertyMapping mapping;
	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	public ElasticsearchIndexFieldType(Class<F> valueType,
			DslConverter<?, ? extends F> dslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory,
			PropertyMapping mapping) {
		this( valueType, dslConverter, projectionConverter,
				codec, predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory,
				mapping, null, null, null );
	}

	public ElasticsearchIndexFieldType(Class<F> valueType,
			DslConverter<?, ? extends F> dslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory,
			PropertyMapping mapping,
			String analyzerName, String searchAnalyzerName, String normalizerName) {
		this.valueType = valueType;
		this.dslConverter = dslConverter;
		this.projectionConverter = projectionConverter;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.mapping = mapping;
		this.analyzerName = analyzerName;
		this.searchAnalyzerName = searchAnalyzerName;
		this.normalizerName = normalizerName;
	}

	@Override
	public String toString() {
		return mapping.toString();
	}

	@Override
	public boolean isSearchable() {
		return predicateBuilderFactory.isSearchable();
	}

	@Override
	public boolean isSortable() {
		return sortBuilderFactory.isSortable();
	}

	@Override
	public boolean isProjectable() {
		return projectionBuilderFactory.isProjectable();
	}

	@Override
	public boolean isAggregable() {
		return aggregationBuilderFactory.isAggregable();
	}

	@Override
	public Class<?> dslArgumentClass() {
		return dslConverter.getValueType();
	}

	@Override
	public Class<?> projectedValueClass() {
		return projectionConverter.getValueType();
	}

	@Override
	public Class<?> valueClass() {
		return valueType;
	}

	@Override
	public Optional<String> analyzerName() {
		return Optional.ofNullable( analyzerName );
	}

	@Override
	public Optional<String> normalizerName() {
		return Optional.ofNullable( normalizerName );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return Optional.ofNullable( searchAnalyzerName );
	}

	public Class<F> getValueType() {
		return valueType;
	}

	public ElasticsearchFieldCodec<F> getCodec() {
		return codec;
	}

	public ElasticsearchFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public ElasticsearchFieldSortBuilderFactory getSortBuilderFactory() {
		return sortBuilderFactory;
	}

	public ElasticsearchFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	public ElasticsearchFieldAggregationBuilderFactory getAggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	public PropertyMapping getMapping() {
		return mapping;
	}
}
