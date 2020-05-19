/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

import org.apache.lucene.analysis.Analyzer;

public class LuceneIndexFieldType<F> implements IndexValueFieldTypeDescriptor, IndexFieldType<F> {
	private final Class<F> valueType;
	private final DslConverter<?, ? extends F> dslConverter;
	private final ProjectionConverter<? super F, ?> projectionConverter;
	private final LuceneFieldCodec<F> codec;
	private final LuceneFieldPredicateBuilderFactory predicateBuilderFactory;
	private final LuceneFieldSortBuilderFactory sortBuilderFactory;
	private final LuceneFieldProjectionBuilderFactory projectionBuilderFactory;
	private final LuceneFieldAggregationBuilderFactory aggregationBuilderFactory;
	private final Analyzer indexingAnalyzerOrNormalizer;
	private final Analyzer searchAnalyzerOrNormalizer;
	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	public LuceneIndexFieldType(Class<F> valueType,
			DslConverter<?, ? extends F> dslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory) {
		this( valueType, dslConverter, projectionConverter,
				codec, predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory,
				null, null, null, null, null );
	}

	public LuceneIndexFieldType(Class<F> valueType,
			DslConverter<?, ? extends F> dslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory,
			Analyzer indexingAnalyzerOrNormalizer, Analyzer searchAnalyzerOrNormalizer,
			String analyzerName, String searchAnalyzerName, String normalizerName) {
		this.valueType = valueType;
		this.dslConverter = dslConverter;
		this.projectionConverter = projectionConverter;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.indexingAnalyzerOrNormalizer = indexingAnalyzerOrNormalizer;
		this.searchAnalyzerOrNormalizer = searchAnalyzerOrNormalizer;
		this.analyzerName = analyzerName;
		this.searchAnalyzerName = searchAnalyzerName;
		this.normalizerName = normalizerName;
	}

	@Override
	public String toString() {
		return "LuceneIndexFieldType["
				+ "codec=" + codec
				+ ", analyzerName=" + analyzerName
				+ ", searchAnalyzerName=" + searchAnalyzerName
				+ ", normalizerName=" + normalizerName
				+ "]";
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

	public LuceneFieldCodec<F> getCodec() {
		return codec;
	}

	public LuceneFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public LuceneFieldSortBuilderFactory getSortBuilderFactory() {
		return sortBuilderFactory;
	}

	public LuceneFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	public LuceneFieldAggregationBuilderFactory getAggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	public Analyzer getIndexingAnalyzerOrNormalizer() {
		return indexingAnalyzerOrNormalizer;
	}

	public Analyzer getSearchAnalyzerOrNormalizer() {
		return searchAnalyzerOrNormalizer;
	}
}
