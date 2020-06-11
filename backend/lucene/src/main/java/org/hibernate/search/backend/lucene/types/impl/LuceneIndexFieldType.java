/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldTypeContext;
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

public class LuceneIndexFieldType<F>
		implements IndexValueFieldTypeDescriptor, IndexFieldType<F>, LuceneSearchFieldTypeContext<F> {
	private final Class<F> valueType;
	private final LuceneFieldCodec<F> codec;
	private final DslConverter<?, ? extends F> dslConverter;
	private final DslConverter<F, ? extends F> rawDslConverter;
	private final ProjectionConverter<? super F, ?> projectionConverter;
	private final ProjectionConverter<? super F, F> rawProjectionConverter;
	private final LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory;
	private final LuceneFieldSortBuilderFactory<F> sortBuilderFactory;
	private final LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory;
	private final LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory;
	private final Analyzer indexingAnalyzerOrNormalizer;
	private final Analyzer searchAnalyzerOrNormalizer;
	private final String analyzerName;
	private final String searchAnalyzerName;
	private final String normalizerName;

	public LuceneIndexFieldType(Class<F> valueType, LuceneFieldCodec<F> codec,
			DslConverter<?, ? extends F> dslConverter,
			DslConverter<F, ? extends F> rawDslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ProjectionConverter<? super F, F> rawProjectionConverter,
			LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory,
			LuceneFieldSortBuilderFactory<F> sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory) {
		this( valueType, codec, dslConverter, rawDslConverter, projectionConverter, rawProjectionConverter,
				predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory,
				null, null, null, null, null );
	}

	public LuceneIndexFieldType(Class<F> valueType, LuceneFieldCodec<F> codec,
			DslConverter<?, ? extends F> dslConverter,
			DslConverter<F, ? extends F> rawDslConverter,
			ProjectionConverter<? super F, ?> projectionConverter,
			ProjectionConverter<? super F, F> rawProjectionConverter,
			LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory,
			LuceneFieldSortBuilderFactory<F> sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory,
			Analyzer indexingAnalyzerOrNormalizer, Analyzer searchAnalyzerOrNormalizer,
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
	public Class<F> valueClass() {
		return valueType;
	}

	public LuceneFieldCodec<F> codec() {
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
	public LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	@Override
	public LuceneFieldSortBuilderFactory<F> sortBuilderFactory() {
		return sortBuilderFactory;
	}

	@Override
	public LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	@Override
	public LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	public Analyzer indexingAnalyzerOrNormalizer() {
		return indexingAnalyzerOrNormalizer;
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return searchAnalyzerOrNormalizer;
	}
}
