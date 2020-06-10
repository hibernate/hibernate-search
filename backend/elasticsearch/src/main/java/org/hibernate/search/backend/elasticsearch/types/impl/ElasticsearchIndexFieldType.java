/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class ElasticsearchIndexFieldType<F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, ElasticsearchSearchFieldTypeContext<F> {
	private final Class<F> valueType;
	private final ElasticsearchFieldCodec<F> codec;
	private final DslConverter<?, ? extends F> dslConverter;
	private final DslConverter<F, ? extends F> rawDslConverter;
	private final ProjectionConverter<? super F, ?> projectionConverter;
	private final ProjectionConverter<? super F, F> rawProjectionConverter;
	private final ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory;
	private final ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory;
	private final ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory;
	private final ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory;
	private final PropertyMapping mapping;
	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	public ElasticsearchIndexFieldType(Class<F> valueType,
			ElasticsearchFieldCodec<F> codec,
			DslConverter<?, ? extends F> dslConverter,
			DslConverter<F, ? extends F> rawDslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ProjectionConverter<? super F, F> rawProjectionConverter,
			ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory,
			PropertyMapping mapping) {
		this( valueType, codec, dslConverter, rawDslConverter, projectionConverter, rawProjectionConverter,
				predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory,
				mapping, null, null, null );
	}

	public ElasticsearchIndexFieldType(Class<F> valueType,
			ElasticsearchFieldCodec<F> codec,
			DslConverter<?, ? extends F> dslConverter,
			DslConverter<F, ? extends F> rawDslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ProjectionConverter<? super F, F> rawProjectionConverter,
			ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory,
			PropertyMapping mapping,
			String analyzerName, String searchAnalyzerName, String normalizerName) {
		this.valueType = valueType;
		this.codec = codec;
		this.dslConverter = dslConverter;
		this.rawDslConverter = rawDslConverter;
		this.projectionConverter = projectionConverter;
		this.rawProjectionConverter = rawProjectionConverter;
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
	public Class<F> valueClass() {
		return valueType;
	}

	public ElasticsearchFieldCodec<F> codec() {
		return codec;
	}

	@Override
	public boolean searchable() {
		return predicateBuilderFactory.isSearchable();
	}

	@Override
	public boolean sortable() {
		return sortBuilderFactory.isSortable();
	}

	@Override
	public boolean projectable() {
		return projectionBuilderFactory.isProjectable();
	}

	@Override
	public boolean aggregable() {
		return aggregationBuilderFactory.isAggregable();
	}

	@Override
	public Class<?> dslArgumentClass() {
		return dslConverter.valueType();
	}

	@Override
	public DslConverter<?, ? extends F> dslConverter() {
		return dslConverter;
	}

	@Override
	public DslConverter<F, ? extends F> rawDslConverter() {
		return rawDslConverter;
	}

	@Override
	public Class<?> projectedValueClass() {
		return projectionConverter.valueType();
	}

	@Override
	public ProjectionConverter<? super F, ?> projectionConverter() {
		return projectionConverter;
	}

	@Override
	public ProjectionConverter<? super F, F> rawProjectionConverter() {
		return rawProjectionConverter;
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

	@Override
	public ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	@Override
	public ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory() {
		return sortBuilderFactory;
	}

	@Override
	public ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	@Override
	public ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	public PropertyMapping mapping() {
		return mapping;
	}
}
