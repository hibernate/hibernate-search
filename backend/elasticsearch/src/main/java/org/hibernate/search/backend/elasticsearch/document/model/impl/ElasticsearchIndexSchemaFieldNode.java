/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;


public class ElasticsearchIndexSchemaFieldNode<F> {

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final String absolutePath;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final ElasticsearchFieldCodec<F> codec;

	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;

	private final ElasticsearchFieldSortBuilderFactory sortBuilderFactory;

	private final ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory;

	private final ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued,
			ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeFieldName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.multiValued = multiValued;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getNestedPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	/**
	 * @return {@code true} if this node is multi-valued in its parent object.
	 */
	public boolean isMultiValued() {
		return multiValued;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", predicateBuilderFactory=" ).append( predicateBuilderFactory )
				.append( ", sortBuilderFactory=" ).append( sortBuilderFactory )
				.append( ", projectionBuilderFactory=" ).append( projectionBuilderFactory )
				.append( "]" );
		return sb.toString();
	}
}
