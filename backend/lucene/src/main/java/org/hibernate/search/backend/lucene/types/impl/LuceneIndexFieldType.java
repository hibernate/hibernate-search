/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import org.apache.lucene.analysis.Analyzer;

public class LuceneIndexFieldType<F> implements IndexFieldType<F> {
	private final LuceneFieldCodec<F> codec;
	private final LuceneFieldPredicateBuilderFactory predicateBuilderFactory;
	private final LuceneFieldSortBuilderFactory sortBuilderFactory;
	private final LuceneFieldProjectionBuilderFactory projectionBuilderFactory;
	private final LuceneFieldAggregationBuilderFactory aggregationBuilderFactory;
	private final boolean aggregable;
	private final Analyzer analyzerOrNormalizer;

	public LuceneIndexFieldType(
			LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory,
			boolean aggregable) {
		this( codec, predicateBuilderFactory, sortBuilderFactory, projectionBuilderFactory,
				aggregationBuilderFactory, aggregable, null );
	}

	public LuceneIndexFieldType(LuceneFieldCodec<F> codec,
			LuceneFieldPredicateBuilderFactory predicateBuilderFactory,
			LuceneFieldSortBuilderFactory sortBuilderFactory,
			LuceneFieldProjectionBuilderFactory projectionBuilderFactory,
			LuceneFieldAggregationBuilderFactory aggregationBuilderFactory,
			boolean aggregable,
			Analyzer analyzerOrNormalizer) {
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.aggregable = aggregable;
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	public LuceneIndexSchemaFieldNode<F> addField(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode, String relativeFieldName, boolean multiValued) {
		LuceneIndexSchemaFieldNode<F> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				relativeFieldName,
				multiValued,
				codec,
				predicateBuilderFactory,
				sortBuilderFactory,
				projectionBuilderFactory,
				aggregationBuilderFactory
		);

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );

		if ( aggregable ) {
			collector.collectFacetConfig( schemaNode.getAbsoluteFieldPath(), multiValued );
		}

		collector.collectAnalyzer( schemaNode.getAbsoluteFieldPath(), analyzerOrNormalizer );

		return schemaNode;
	}
}
