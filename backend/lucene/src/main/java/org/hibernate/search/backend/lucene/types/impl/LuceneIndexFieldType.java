/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import org.apache.lucene.analysis.Analyzer;

public class LuceneIndexFieldType<F> implements IndexFieldType<F> {
	private final Class<F> valueType;
	private final LuceneFieldCodec<F> codec;
	private final LuceneFieldPredicateBuilderFactory predicateBuilderFactory;
	private final LuceneFieldSortBuilderFactory sortBuilderFactory;
	private final LuceneFieldProjectionBuilderFactory projectionBuilderFactory;
	private final LuceneFieldAggregationBuilderFactory aggregationBuilderFactory;
	private final Analyzer analyzerOrNormalizer;

	public LuceneIndexFieldType(Class<F> valueType, LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory) {
		this( valueType, codec, predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory, null );
	}

	public LuceneIndexFieldType(Class<F> valueType, LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory,
			Analyzer analyzerOrNormalizer) {
		this.valueType = valueType;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public String toString() {
		return "LuceneIndexFieldType["
				+ "codec=" + codec
				+ ", analyzerOrNormalizer=" + analyzerOrNormalizer
				+ "]";
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

	public Analyzer getAnalyzerOrNormalizer() {
		return analyzerOrNormalizer;
	}
}
