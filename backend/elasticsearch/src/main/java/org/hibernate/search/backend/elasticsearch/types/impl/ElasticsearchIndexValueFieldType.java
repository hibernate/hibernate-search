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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

public class ElasticsearchIndexValueFieldType<F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, ElasticsearchSearchValueFieldTypeContext<F> {
	private final Class<F> valueType;
	private final DslConverter<F, F> rawDslConverter;
	private final ProjectionConverter<F, F> rawProjectionConverter;

	private final ElasticsearchFieldCodec<F> codec;
	private final DslConverter<?, F> dslConverter;
	private final ProjectionConverter<F, ?> projectionConverter;

	private final boolean searchable;
	private final boolean sortable;
	private final boolean projectable;
	private final boolean aggregable;

	private final Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchValueFieldQueryElementFactory<?, F>> queryElementFactories;

	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	private final PropertyMapping mapping;

	public ElasticsearchIndexValueFieldType(Builder<F> builder) {
		this.valueType = builder.valueType;
		this.rawDslConverter = builder.rawDslConverter;
		this.rawProjectionConverter = builder.rawProjectionConverter;
		this.codec = builder.codec;
		this.dslConverter = builder.dslConverter != null ? builder.dslConverter : rawDslConverter;
		this.projectionConverter = builder.projectionConverter != null ? builder.projectionConverter : rawProjectionConverter;
		this.searchable = builder.searchable;
		this.sortable = builder.sortable;
		this.projectable = builder.projectable;
		this.aggregable = builder.aggregable;
		this.queryElementFactories = builder.queryElementFactories;
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
		return searchable;
	}

	@Override
	public boolean sortable() {
		return sortable;
	}

	@Override
	public boolean projectable() {
		return projectable;
	}

	@Override
	public boolean aggregable() {
		return aggregable;
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
	public boolean hasNormalizerOnAtLeastOneIndex() {
		return normalizerName().isPresent();
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return Optional.ofNullable( searchAnalyzerName );
	}

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public <T> ElasticsearchSearchValueFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (ElasticsearchSearchValueFieldQueryElementFactory<T, F>) queryElementFactories.get( key );
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

		private boolean searchable;
		private boolean sortable;
		private boolean projectable;
		private boolean aggregable;

		private final Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchValueFieldQueryElementFactory<?, F>>
				queryElementFactories = new HashMap<>();

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

		public void searchable(boolean searchable) {
			this.searchable = searchable;
		}

		public void sortable(boolean sortable) {
			this.sortable = sortable;
		}

		public void projectable(boolean projectable) {
			this.projectable = projectable;
		}

		public void aggregable(boolean aggregable) {
			this.aggregable = aggregable;
		}

		public <T> void queryElementFactory(SearchQueryElementTypeKey<T> key,
				ElasticsearchSearchValueFieldQueryElementFactory<T, F> factory) {
			queryElementFactories.put( key, factory );
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

		public ElasticsearchIndexValueFieldType<F> build() {
			return new ElasticsearchIndexValueFieldType<>( this );
		}
	}
}
