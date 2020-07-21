/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldTypeContext;
import org.hibernate.search.backend.lucene.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughFromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.PassThroughToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

import org.apache.lucene.analysis.Analyzer;

public class LuceneIndexValueFieldType<F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, LuceneSearchValueFieldTypeContext<F> {
	private final Class<F> valueType;
	private final DslConverter<F, F> rawDslConverter;
	private final ProjectionConverter<F, F> rawProjectionConverter;

	private final LuceneFieldCodec<F> codec;
	private final DslConverter<?, F> dslConverter;
	private final ProjectionConverter<F, ?> projectionConverter;

	private final boolean searchable;
	private final boolean sortable;
	private final boolean projectable;
	private final boolean aggregable;

	private final Map<SearchQueryElementTypeKey<?>, LuceneSearchFieldQueryElementFactory<?, F>> queryElementFactories;

	private final Analyzer indexingAnalyzerOrNormalizer;
	private final Analyzer searchAnalyzerOrNormalizer;
	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	private LuceneIndexValueFieldType(Builder<F> builder) {
		this.valueType = builder.valueType;
		this.rawDslConverter = builder.rawDslConverter;
		this.rawProjectionConverter = builder.rawProjectionConverter;
		this.codec = builder.codec;
		this.dslConverter = builder.dslConverter != null ? builder.dslConverter : rawDslConverter;
		this.projectionConverter = builder.projectionConverter != null ? builder.projectionConverter
				: rawProjectionConverter;
		this.searchable = builder.searchable;
		this.sortable = builder.sortable;
		this.projectable = builder.projectable;
		this.aggregable = builder.aggregable;
		this.queryElementFactories = builder.queryElementFactories;
		this.indexingAnalyzerOrNormalizer = builder.indexingAnalyzerOrNormalizer();
		this.searchAnalyzerOrNormalizer = builder.searchAnalyzer != null ? builder.searchAnalyzer
				: indexingAnalyzerOrNormalizer;
		this.analyzerName = builder.analyzerName;
		this.searchAnalyzerName = builder.searchAnalyzerName != null ? builder.searchAnalyzerName
				: analyzerName;
		this.normalizerName = builder.normalizerName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "codec=" + codec
				+ ", analyzerName=" + analyzerName
				+ ", searchAnalyzerName=" + searchAnalyzerName
				+ ", normalizerName=" + normalizerName
				+ ", capabilities=" + queryElementFactories.keySet()
				+ "]";
	}

	@Override
	public Class<F> valueClass() {
		return valueType;
	}

	public LuceneFieldCodec<F> codec() {
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
	public Optional<String> searchAnalyzerName() {
		return Optional.ofNullable( searchAnalyzerName );
	}

	@SuppressWarnings("unchecked") // The cast is safe by construction; see the builder.
	@Override
	public <T> LuceneSearchFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (LuceneSearchFieldQueryElementFactory<T, F>) queryElementFactories.get( key );
	}

	public Analyzer indexingAnalyzerOrNormalizer() {
		return indexingAnalyzerOrNormalizer;
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return searchAnalyzerOrNormalizer;
	}

	public static class Builder<F> {

		private final Class<F> valueType;
		private final DslConverter<F, F> rawDslConverter;
		private final ProjectionConverter<F, F> rawProjectionConverter;

		private LuceneFieldCodec<F> codec;
		private DslConverter<?, F> dslConverter;
		private ProjectionConverter<F, ?> projectionConverter;

		private boolean searchable;
		private boolean sortable;
		private boolean projectable;
		private boolean aggregable;

		private final Map<SearchQueryElementTypeKey<?>, LuceneSearchFieldQueryElementFactory<?, F>>
				queryElementFactories = new HashMap<>();

		private Analyzer analyzer;
		private String analyzerName;

		private Analyzer searchAnalyzer;
		private String searchAnalyzerName;

		private Analyzer normalizer;
		private String normalizerName;

		public Builder(Class<F> valueType) {
			this.valueType = valueType;
			this.rawDslConverter = new DslConverter<>( valueType, new PassThroughToDocumentFieldValueConverter<>() );
			this.rawProjectionConverter = new ProjectionConverter<>( valueType, new PassThroughFromDocumentFieldValueConverter<>() );
		}

		public void codec(LuceneFieldCodec<F> codec) {
			this.codec = codec;
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
				LuceneSearchFieldQueryElementFactory<T, F> factory) {
			queryElementFactories.put( key, factory );
		}

		public void analyzer(String analyzerName, Analyzer analyzer) {
			this.analyzerName = analyzerName;
			this.analyzer = analyzer;
		}

		public void searchAnalyzer(String searchAnalyzerName, Analyzer searchAnalyzer) {
			this.searchAnalyzerName = searchAnalyzerName;
			this.searchAnalyzer = searchAnalyzer;
		}

		public void normalizer(String normalizerName, Analyzer normalizer) {
			this.normalizerName = normalizerName;
			this.normalizer = normalizer;
		}

		public Analyzer indexingAnalyzerOrNormalizer() {
			return analyzer != null ? analyzer : normalizer;
		}

		public LuceneIndexValueFieldType<F> build() {
			return new LuceneIndexValueFieldType<>( this );
		}
	}
}
