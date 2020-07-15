/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class ElasticsearchIndexFieldType<F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, ElasticsearchSearchFieldTypeContext<F> {
	private final Class<F> valueType;
	private final DslConverter<F, F> rawDslConverter;
	private final ProjectionConverter<F, F> rawProjectionConverter;

	private final ElasticsearchFieldCodec<F> codec;
	private final DslConverter<?, F> dslConverter;
	private final ProjectionConverter<F, ?> projectionConverter;

	private final Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchFieldQueryElementFactory<?, F>> queryElementFactories;

	private final ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory;
	private final ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory;
	private final ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory;
	private final ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory;

	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	private final PropertyMapping mapping;

	public ElasticsearchIndexFieldType(Builder<F> builder) {
		this.valueType = builder.valueType;
		this.rawDslConverter = builder.rawDslConverter;
		this.rawProjectionConverter = builder.rawProjectionConverter;
		this.codec = builder.codec;
		this.dslConverter = builder.dslConverter != null ? builder.dslConverter : rawDslConverter;
		this.projectionConverter = builder.projectionConverter != null ? builder.projectionConverter : rawProjectionConverter;
		this.queryElementFactories = builder.queryElementFactories;
		this.predicateBuilderFactory = builder.predicateBuilderFactory;
		this.sortBuilderFactory = builder.sortBuilderFactory;
		this.projectionBuilderFactory = builder.projectionBuilderFactory;
		this.aggregationBuilderFactory = builder.aggregationBuilderFactory;
		this.analyzerName = builder.analyzerName;
		this.searchAnalyzerName = builder.searchAnalyzerName != null ? builder.searchAnalyzerName : builder.analyzerName;
		this.normalizerName = builder.normalizerName;
		this.mapping = builder.mapping;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "mapping=" + mapping.toString()
				+ ", capabilities=" + queryElementFactories.keySet()
				+ "]";
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
	public DslConverter<?, F> dslConverter() {
		return dslConverter;
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return rawDslConverter;
	}

	@Override
	public Class<?> projectedValueClass() {
		return projectionConverter.valueType();
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return projectionConverter;
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
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

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public <T> ElasticsearchSearchFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (ElasticsearchSearchFieldQueryElementFactory<T, F>) queryElementFactories.get( key );
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

	public static class Builder<F> {

		private final Class<F> valueType;
		private final DslConverter<F, F> rawDslConverter;
		private final ProjectionConverter<F, F> rawProjectionConverter;

		private ElasticsearchFieldCodec<F> codec;
		private DslConverter<?, F> dslConverter;
		private ProjectionConverter<F, ?> projectionConverter;

		private final Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchFieldQueryElementFactory<?, F>>
				queryElementFactories = new HashMap<>();

		private ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory;
		private ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory;
		private ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory;
		private ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory;

		private String analyzerName;
		private String searchAnalyzerName;
		private String normalizerName;

		private final PropertyMapping mapping;

		public Builder(Class<F> valueType, PropertyMapping mapping) {
			this.valueType = valueType;
			this.rawDslConverter = new DslConverter<>( valueType, new PassThroughToDocumentFieldValueConverter<>() );
			this.rawProjectionConverter = new ProjectionConverter<>( valueType, new PassThroughFromDocumentFieldValueConverter<>() );
			this.mapping = mapping;
		}

		public Class<F> valueType() {
			return valueType;
		}

		public void codec(ElasticsearchFieldCodec<F> codec) {
			this.codec = codec;
		}

		public ElasticsearchFieldCodec<F> codec() {
			return codec;
		}

		public <V> void dslConverter(Class<V> valueType, ToDocumentFieldValueConverter<V, ? extends F> toIndexConverter) {
			this.dslConverter = new DslConverter<>( valueType, toIndexConverter );
		}

		public <V> void projectionConverter(Class<V> valueType, FromDocumentFieldValueConverter<? super F, V> fromIndexConverter) {
			this.projectionConverter = new ProjectionConverter<>( valueType, fromIndexConverter );
		}

		public <T> void queryElementFactory(SearchQueryElementTypeKey<T> key,
				ElasticsearchSearchFieldQueryElementFactory<T, F> factory) {
			queryElementFactories.put( key, factory );
		}

		public void predicateBuilderFactory(ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory) {
			this.predicateBuilderFactory = predicateBuilderFactory;
		}

		public void sortBuilderFactory(ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory) {
			this.sortBuilderFactory = sortBuilderFactory;
		}

		public void projectionBuilderFactory(ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory) {
			this.projectionBuilderFactory = projectionBuilderFactory;
		}

		public void aggregationBuilderFactory(ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory) {
			this.aggregationBuilderFactory = aggregationBuilderFactory;
		}

		public void analyzerName(String analyzerName) {
			this.analyzerName = analyzerName;
		}

		public void searchAnalyzerName(String searchAnalyzerName) {
			this.searchAnalyzerName = searchAnalyzerName;
		}

		public void normalizerName(String normalizerName) {
			this.normalizerName = normalizerName;
		}

		public PropertyMapping mapping() {
			return mapping;
		}

		public ElasticsearchIndexFieldType<F> build() {
			return new ElasticsearchIndexFieldType<>( this );
		}
	}
}
